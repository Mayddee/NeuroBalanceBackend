"""
Neural Balance ML Service - ULTIMATE Edition
Version: 5.1.0  (+ Swagger UI via flasgger, + /recommend/single endpoint)
"""

from flask import Flask, request, jsonify, Response
from flask_cors import CORS
from flasgger import Swagger
import numpy as np
import pandas as pd
import joblib
from catboost import CatBoostRegressor
import logging
from datetime import datetime, timezone
from pathlib import Path
from zoneinfo import ZoneInfo
import json

# ── Per-user recommendation cache (in-memory, keyed by user_id) ──────────────
# Populated when Java backend calls /recommend/top3 with user_id (Kafka-triggered events).
# Frontend calls /recommend/top3 with user_id → gets cached result instead of defaults.
# Cache is per-day (Asia/Almaty); new day = stale = recompute.
_ALMATY = ZoneInfo('Asia/Almaty')
_user_cache: dict = {}  # {str(user_id): {'date': 'YYYY-MM-DD', 'result': dict}}


def _today_almaty() -> str:
    return datetime.now(_ALMATY).date().isoformat()


def convert_to_serializable(obj):
    """Convert numpy types to standard Python types for JSON serialization."""
    if isinstance(obj, np.integer):
        return int(obj)
    if isinstance(obj, np.floating):
        return float(obj)
    if isinstance(obj, np.ndarray):
        return obj.tolist()
    if isinstance(obj, dict):
        return {k: convert_to_serializable(v) for k, v in obj.items()}
    if isinstance(obj, list):
        return [convert_to_serializable(i) for i in obj]
    return obj


# ═══════════════════════════════════════════════════════════
# 1. MODEL CLASS
# ═══════════════════════════════════════════════════════════

class SmartRecommendationSelector:
    def __init__(self, catboost_model, xgb_regression_model, feature_names):
        self.catboost = catboost_model
        self.xgb = xgb_regression_model
        self.features = feature_names
        self.cb_features = list(catboost_model.feature_names_)

        self.rules = {
            'SLEEP_INCREASE': {
                'title': '🌙 Оптимизация сна',
                'basis': 'Walker (2017): Why We Sleep',
                'actions': {
                    0.5: ['Лягте на 30 минут раньше сегодня'],
                    1.0: ['Будильник на отбой в 23:00', 'Затемните спальню'],
                    1.5: ['Вечерний ритуал 21:30', 'Кофеин только до 15:00'],
                    2.0: ['Режим 23:00-7:00 (8h)', 'T=18°C', 'Телефон вне спальни'],
                    2.5: ['Строгий режим', 'Мелатонин 0.5mg', 'Ортопедическая подушка'],
                    3.0: ['Консультация сомнолога', 'Полисомнография']
                }
            },
            'STRESS_DECREASE': {
                'title': '🧘 Управление стрессом',
                'basis': 'McEwen (2007): Allostatic Load',
                'actions': {
                    -1: ['3 глубоких вдоха прямо сейчас (4-6-4)'],
                    -2: ['Медитация 10 мин утром', 'Прогулка 15 мин обед'],
                    -3: ['Pomodoro 25/5', 'Делегировать 2 задачи', 'Йога вечером'],
                    -4: ['Психолог КПТ', 'Прогрессивная релаксация', 'Отказ от 1 обязательства'],
                    -5: ['Когнитивная терапия', 'MBSR курс 8 недель', 'Sabbatical месяц']
                }
            },
            'EXERCISE_INCREASE': {
                'title': '💪 Физическая активность',
                'basis': 'Ratey (2008): Spark - BDNF',
                'actions': {
                    1: ['Ходьба 30 мин × 3/неделю'],
                    2: ['2 силовые 20 мин', 'Лестница > лифт'],
                    3: ['4 раза: 2 кардио + 2 силовые', 'Спортклуб'],
                    4: ['5-6/неделю', 'HIIT 2 раза', 'Плавание 1 раз'],
                    5: ['Личный тренер', 'Кроссфит', 'Полумарафон цель']
                }
            },
            'SCREEN_DECREASE': {
                'title': '📱 Цифровой детокс',
                'basis': 'Newport (2016): Deep Work',
                'actions': {
                    -1: ['Лимит соцсети 1h/день'],
                    -2: ['20-20-20 правило', 'Уведомления OFF'],
                    -3: ['Ч/б режим 20:00+', 'Чтение 30 мин вместо IG'],
                    -4: ['Цифровой закат 21:00', 'Удалить TikTok+Twitter', 'Forest app'],
                    -5: ['Digital Sabbath (суббота без гаджетов)', 'Кнопочный телефон'],
                    -6: ['Полный детокс 30 дней', 'Flip phone', 'Только ноутбук для работы']
                }
            }
        }

    def select_top_3(self, user_data, scaler):
        user_df = pd.DataFrame([user_data]).reindex(columns=self.features, fill_value=0)
        current_scaled = scaler.transform(user_df)
        current_score = float(self.xgb.predict(current_scaled)[0])

        all_recommendations = []
        for atype in self.rules.keys():
            best_rec = self._find_best_for_type(atype, user_data, current_score)
            if best_rec:
                all_recommendations.append(best_rec)

        all_recommendations.sort(key=lambda x: x['predicted_improvement'], reverse=True)
        top_3 = all_recommendations[:3]

        return {
            'cognitive_score': round(current_score, 1),
            'recommendations': top_3,
            'total_potential': round(sum(r['predicted_improvement'] for r in top_3), 1),
            'summary': (f"Ваш когнитивный счет: {current_score:.1f}. "
                        f"Рекомендации могут дать +{sum(r['predicted_improvement'] for r in top_3):.1f} балла.")
        }

    def _find_best_for_type(self, action_type, user_data, current_score):
        rule = self.rules[action_type]
        deltas = list(rule['actions'].keys())
        param_map = {
            'SLEEP_INCREASE': 'sleep_duration',
            'STRESS_DECREASE': 'stress_level',
            'EXERCISE_INCREASE': 'exercise_frequency_num',
            'SCREEN_DECREASE': 'daily_screen_time'
        }
        baseline = user_data.get(param_map[action_type], 5)
        candidates = []
        for d in deltas:
            new_v = np.clip(baseline + d, 0, 16)
            imp = self._predict_impact(user_data, action_type, d, baseline, new_v, current_score)
            candidates.append({'delta': d, 'new_v': new_v, 'imp': imp})
        best = max(candidates, key=lambda x: x['imp'])
        return {
            'type': action_type,
            'title': rule['title'],
            'predicted_improvement': round(best['imp'], 1),
            'priority': 'CRITICAL' if best['imp'] > 10 else ('HIGH' if best['imp'] > 6 else 'MEDIUM'),
            'actions': rule['actions'][best['delta']],
            'scientific_basis': rule['basis'],
            'baseline': round(baseline, 1),
            'recommended_target': round(best['new_v'], 1)
        }

    def _predict_impact(self, user_data, action_type, delta, baseline, new_value, current_score):
        row = {k.lower(): v for k, v in user_data.items()}
        row.update({
            'action_delta': float(delta),
            'baseline_value': float(baseline),
            'new_value': float(new_value),
            'baseline_score': float(current_score),
            'is_critical': 1.0 if self._check_critical(action_type, baseline) else 0.0,
            'is_optimal_zone': 1.0 if self._check_optimal(action_type, new_value) else 0.0
        })
        for atype in ['SLEEP_INCREASE', 'STRESS_DECREASE', 'EXERCISE_INCREASE', 'SCREEN_DECREASE']:
            row[f'action_{atype}'] = 1.0 if atype == action_type else 0.0
        X_tmp = pd.DataFrame([row]).reindex(columns=self.cb_features, fill_value=0.0)
        return float(self.catboost.predict(X_tmp)[0])

    def _check_critical(self, atype, val):
        crit_map = {'SLEEP_INCREASE': 6.5, 'STRESS_DECREASE': 7, 'EXERCISE_INCREASE': 2, 'SCREEN_DECREASE': 9}
        return val < crit_map[atype] if 'INCREASE' in atype else val > crit_map[atype]

    def _check_optimal(self, atype, val):
        if atype == 'SLEEP_INCREASE': return 7.5 <= val <= 9
        if atype == 'STRESS_DECREASE': return val <= 3
        if atype == 'EXERCISE_INCREASE': return val >= 4
        if atype == 'SCREEN_DECREASE': return val <= 4
        return False


# ═══════════════════════════════════════════════════════════
# 2. SETUP & MODEL LOADING
# ═══════════════════════════════════════════════════════════

app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False
CORS(app)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Swagger UI (accessible at /swagger/)
swagger = Swagger(app, template={
    "info": {
        "title": "NeuroBalance ML Service",
        "description": "ML API: cognitive score prediction and lifestyle recommendations (CatBoost + XGBoost)",
        "version": "5.1.0",
    },
    "host": "localhost:5001",
    "basePath": "/",
    "schemes": ["http"],
})

BASE_DIR = Path(__file__).parent
MODEL_DIR = BASE_DIR / 'notebooks' / 'models'

try:
    xgb_reg = joblib.load(MODEL_DIR / 'cognitive_score_model.pkl')
    scaler = joblib.load(MODEL_DIR / 'scaler.pkl')
    label_encoder = joblib.load(MODEL_DIR / 'label_encoder.pkl')

    catboost_model = CatBoostRegressor()
    catboost_model.load_model(str(MODEL_DIR / 'catboost_recommendation_model.cbm'))

    with open(MODEL_DIR / 'feature_names.txt', 'r') as f:
        FINAL_FEATURES = [line.strip() for line in f.readlines()]

    smart_selector = SmartRecommendationSelector(catboost_model, xgb_reg, FINAL_FEATURES)
    logger.info("✅ All models loaded successfully!")
except Exception as e:
    logger.error(f"❌ Initialization error: {e}")
    raise

# Health-metrics XGBoost models (optional — loaded only if notebook was run)
HM_FEATURES = [
    'sleep_hours', 'sleep_quality', 'energy_level', 'morning_mood',
    'evening_mood', 'stress_level', 'did_exercise', 'ate_healthy',
    'had_social_interaction', 'felt_rested', 'deep_sleep_minutes',
    'rem_sleep_minutes', 'total_sleep_minutes', 'sleep_debt',
    'sleep_efficiency', 'wellbeing_index', 'habit_count'
]

_hm_models = {}
_hm_scalers = {}
try:
    for _t in ['m_rest', 'm_ready', 'm_balance']:
        _hm_models[_t]  = joblib.load(MODEL_DIR / f'hm_{_t}_model.pkl')
        _hm_scalers[_t] = joblib.load(MODEL_DIR / f'hm_{_t}_scaler.pkl')
    logger.info("✅ Health-metrics ML models loaded!")
except Exception:
    logger.warning("⚠️  Health-metrics ML models not found — /health-metrics/ml-predict will use formula fallback")


# ═══════════════════════════════════════════════════════════
# 3. HELPERS
# ═══════════════════════════════════════════════════════════

def calc_engineered_features(data):
    res = data.copy()
    # FIX: exercise_frequency_num must be derived FIRST — it is used in multiple calculations below.
    # Previously it was set at the END, so all intermediate calculations used the default (3).
    res['exercise_frequency_num'] = data.get('exercise_frequency', 3)

    r_norm = (data.get('reaction_time', 350) - 200) / 400
    s_norm = (data.get('stress_level', 5) - 1) / 9
    sleep_debt = max(0, 7 - data.get('sleep_duration', 7)) / 3
    screen_fat = max(0, data.get('daily_screen_time', 8) - 8) / 4
    res['cfi'] = (0.30 * r_norm + 0.25 * s_norm + 0.25 * sleep_debt + 0.20 * screen_fat) * 100
    res['sleep_debt'] = max(0, 7 - data.get('sleep_duration', 7))
    res['memory_efficiency'] = (data.get('memory_test_score', 70) / data.get('reaction_time', 350)) * 1000
    sleep_sc = np.clip((data.get('sleep_duration', 7) - 4) / 6, 0, 1)
    ex_sc = np.clip(res['exercise_frequency_num'] / 7, 0, 1)   # now uses the actual value, not default
    st_sc = 1 - ((data.get('stress_level', 5) - 1) / 9)
    scr_sc = 1 - np.clip((data.get('daily_screen_time', 8) - 1) / 11, 0, 1)
    res['lifestyle_balance'] = (0.30 * sleep_sc + 0.25 * st_sc + 0.20 * ex_sc + 0.15 * scr_sc) * 100
    res['sleep_exercise_interaction'] = data.get('sleep_duration', 7) * res['exercise_frequency_num']
    return res


# ═══════════════════════════════════════════════════════════
# 4. ENDPOINTS
# ═══════════════════════════════════════════════════════════

@app.route('/health', methods=['GET'])
def health():
    """
    Health check
    ---
    tags:
      - System
    responses:
      200:
        description: Service is healthy
        schema:
          type: object
          properties:
            status:
              type: string
              example: healthy
            version:
              type: string
              example: 5.1.0
    """
    return jsonify({'status': 'healthy', 'version': '5.1.0'}), 200


@app.route('/predict', methods=['POST'])
def predict():
    """
    Predict cognitive score and state
    ---
    tags:
      - Prediction
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            sleep_duration:
              type: number
              example: 7.0
              description: Hours of sleep per night
            stress_level:
              type: number
              example: 5.0
              description: Stress level (1-10)
            daily_screen_time:
              type: number
              example: 8.0
            exercise_frequency:
              type: number
              example: 3.0
            caffeine_intake:
              type: number
              example: 2.0
            reaction_time:
              type: number
              example: 300.0
              description: Reaction time in ms
            memory_test_score:
              type: number
              example: 75.0
            age:
              type: number
              example: 25.0
            gender:
              type: string
              example: Male
            diet_type:
              type: string
              example: Non-Vegetarian
    responses:
      200:
        description: Cognitive score prediction
        schema:
          type: object
          properties:
            status:
              type: string
              example: success
            cognitive_score:
              type: number
              example: 72.4
            cognitive_state:
              type: string
              example: Medium
              description: "High (>75) / Medium (50-75) / Low (<50)"
            cfi_score:
              type: number
              example: 31.2
              description: Cognitive Fatigue Index
            timestamp:
              type: string
      400:
        description: No data provided
      500:
        description: Internal server error
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        user_data = calc_engineered_features(data)

        user_df = pd.DataFrame([user_data]).reindex(columns=smart_selector.features, fill_value=0)
        vec_scaled = scaler.transform(user_df)
        score = float(xgb_reg.predict(vec_scaled)[0])

        state = "Low"
        if score > 75:
            state = "High"
        elif score > 50:
            state = "Medium"

        response_data = {
            'status': 'success',
            'cognitive_score': round(score, 1),
            'cognitive_state': state,
            'cfi_score': round(user_data.get('cfi', 0), 1),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }

        return Response(
            json.dumps(response_data, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"❌ Error in /predict: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/recommend/top3', methods=['POST'])
def recommend_top3():
    """
    Get top-3 lifestyle recommendations
    ---
    tags:
      - Recommendations
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            sleep_duration:
              type: number
              example: 6.0
            stress_level:
              type: number
              example: 7.0
            daily_screen_time:
              type: number
              example: 9.0
            exercise_frequency:
              type: number
              example: 2.0
            caffeine_intake:
              type: number
              example: 3.0
            reaction_time:
              type: number
              example: 350.0
            memory_test_score:
              type: number
              example: 65.0
            age:
              type: number
              example: 25.0
            gender:
              type: string
              example: Male
            diet_type:
              type: string
              example: Non-Vegetarian
    responses:
      200:
        description: Top-3 recommendations with cognitive score
        schema:
          type: object
          properties:
            status:
              type: string
              example: success
            cognitive_score:
              type: number
              example: 58.3
            recommendations:
              type: array
              items:
                type: object
            total_potential:
              type: number
              example: 24.5
            summary:
              type: string
      500:
        description: Internal server error
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        user_id = str(data.get('user_id', '')) or None
        # internal=True means Java backend (Kafka event) — always recompute + update cache
        is_internal = bool(data.get('internal', False))

        # ── Cache hit: return stored result from last Kafka-triggered computation ──
        # Only for frontend calls (internal=False). Java calls always recompute.
        if user_id and not is_internal:
            cached = _user_cache.get(user_id)
            if cached and cached.get('date') == _today_almaty():
                logger.info(f"ML cache hit for user {user_id}")
                return Response(
                    json.dumps({'status': 'success', **cached['result']}, ensure_ascii=False),
                    mimetype='application/json',
                    headers={'Content-Type': 'application/json; charset=utf-8'}
                )

        # ── Compute: Java internal (real data) or frontend first-time (defaults) ──
        user_data = calc_engineered_features(data)
        result = smart_selector.select_top_3(user_data, scaler)
        clean_result = convert_to_serializable(result)

        # Cache result for this user (always update when internal, first-time when frontend)
        if user_id:
            _user_cache[user_id] = {'date': _today_almaty(), 'result': clean_result}
            source = "internal-java" if is_internal else "first-call"
            logger.info(f"ML cache updated for user {user_id} (source={source}, score={result.get('cognitive_score', '?')})")

        return Response(
            json.dumps({'status': 'success', **clean_result}, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"❌ Error in /recommend/top3: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/internal/cache/update', methods=['POST'])
def internal_cache_update():
    """
    Internal endpoint — NOT for frontend.
    Java backend calls this after every Kafka-triggered ML recomputation
    (check-in, sleep, game, mood) to push real computed results into Python's cache.
    Next frontend call to /recommend/top3 with the same user_id returns this result.
    ---
    tags:
      - Internal
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            user_id:
              type: integer
              example: 5
            result:
              type: object
    responses:
      200:
        description: Cache updated
      400:
        description: Missing user_id
    """
    try:
        data = request.get_json()
        if not data or 'user_id' not in data:
            return jsonify({'error': 'user_id required'}), 400

        user_id = str(data['user_id'])
        result = data.get('result', {})

        _user_cache[user_id] = {'date': _today_almaty(), 'result': result}
        logger.info(f"ML cache updated via internal push for user {user_id}")
        return jsonify({'status': 'ok', 'user_id': user_id}), 200
    except Exception as e:
        logger.error(f"Error in /internal/cache/update: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/recommend/single', methods=['POST'])
def recommend_single():
    """
    Get the single best recommendation
    ---
    tags:
      - Recommendations
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            sleep_duration:
              type: number
              example: 5.5
            stress_level:
              type: number
              example: 8.0
            daily_screen_time:
              type: number
              example: 10.0
            exercise_frequency:
              type: number
              example: 1.0
            caffeine_intake:
              type: number
              example: 4.0
            reaction_time:
              type: number
              example: 380.0
            memory_test_score:
              type: number
              example: 60.0
            age:
              type: number
              example: 28.0
            gender:
              type: string
              example: Female
            diet_type:
              type: string
              example: Vegetarian
    responses:
      200:
        description: Single best recommendation (array with 1 item)
        schema:
          type: object
          properties:
            status:
              type: string
              example: success
            cognitive_score:
              type: number
              example: 52.1
            recommendations:
              type: array
              items:
                type: object
              description: Array containing the single best recommendation
            total_potential:
              type: number
              example: 12.3
            summary:
              type: string
      500:
        description: Internal server error
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        user_data = calc_engineered_features(data)
        result = smart_selector.select_top_3(user_data, scaler)

        best = result['recommendations'][:1]
        clean_result = convert_to_serializable({
            'cognitive_score': result['cognitive_score'],
            'recommendations': best,
            'total_potential': best[0]['predicted_improvement'] if best else 0,
            'summary': result['summary'],
        })

        return Response(
            json.dumps({'status': 'success', **clean_result}, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"❌ Error in /recommend/single: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


# ═══════════════════════════════════════════════════════════
# 5. NEUROBALANCE HEALTH METRICS ENDPOINTS
#    M-Rest / M-Ready / M-Balance — формулы идентичны
#    HealthMetrics.java (верифицированы с мобильным приложением)
# ═══════════════════════════════════════════════════════════

def calc_m_rest(data: dict) -> float:
    """
    M-Rest = качество восстановления и сна (0-100).
    Формула:
      durationScore  = min(sleepHours / 8.0, 1.0) * 50
      qualityScore   = если deep+rem известны: min((deep+rem)/total/0.40, 1.0)*30
                       иначе: (sleepQuality / 10.0) * 30
      subjectiveScore = feltRested: 20 (true) / 10 (false) / 20 (null)
    """
    hours = float(data.get('sleep_hours', 7.0))
    duration_score = min(hours / 8.0, 1.0) * 50.0

    deep = data.get('deep_sleep_minutes')
    rem = data.get('rem_sleep_minutes')
    total_min = data.get('total_sleep_minutes')
    sleep_quality = data.get('sleep_quality')

    if deep is not None and rem is not None and total_min and total_min > 0:
        deep_rem = float(deep) + float(rem)
        quality_score = min((deep_rem / float(total_min)) / 0.40, 1.0) * 30.0
    elif sleep_quality is not None:
        quality_score = (float(sleep_quality) / 10.0) * 30.0
    else:
        quality_score = 15.0

    felt_rested = data.get('felt_rested')
    if felt_rested is True:
        subjective_score = 20.0
    elif felt_rested is False:
        subjective_score = 10.0
    else:
        subjective_score = 20.0

    result = duration_score + quality_score + subjective_score
    return max(0.0, min(100.0, result))


def calc_m_ready(data: dict, m_rest: float) -> float:
    """
    M-Ready = когнитивная готовность к дню (0-100).
    Формула:
      energyScore = (energyLevel / 10.0) * 40
      moodScore   = (morningMood / 5.0) * 30
      restScore   = mRest * 0.30
    """
    energy = data.get('energy_level')
    morning_mood = data.get('morning_mood')

    energy_score = (float(energy) / 10.0) * 40.0 if energy is not None else 20.0
    mood_score = (float(morning_mood) / 5.0) * 30.0 if morning_mood is not None else 15.0
    rest_score = m_rest * 0.30

    result = energy_score + mood_score + rest_score
    return max(0.0, min(100.0, result))


def calc_m_balance(data: dict) -> float:
    """
    M-Balance = эмоциональный баланс (0-100).
    Формула:
      stressScore = max((10 - stressLevel) / 9.0, 0) * 40
      habitsScore = didExercise(13.3) + ateHealthy(13.3) + hadSocialInteraction(13.4)
      moodScore   = (eveningMood / 5.0) * 20
    """
    stress = data.get('stress_level', 5)
    stress_score = max((10.0 - float(stress)) / 9.0, 0.0) * 40.0

    habits_score = 0.0
    if data.get('did_exercise') is True:
        habits_score += 13.3
    if data.get('ate_healthy') is True:
        habits_score += 13.3
    if data.get('had_social_interaction') is True:
        habits_score += 13.4

    evening_mood = data.get('evening_mood')
    mood_score = (float(evening_mood) / 5.0) * 20.0 if evening_mood is not None else 10.0

    result = stress_score + habits_score + mood_score
    return max(0.0, min(100.0, result))


def score_label(score: float) -> str:
    if score >= 80:
        return 'Excellent'
    if score >= 60:
        return 'Good'
    if score >= 40:
        return 'Fair'
    return 'Poor'


@app.route('/health-metrics/calculate', methods=['POST'])
def calculate_health_metrics():
    """
    Calculate M-Rest, M-Ready, M-Balance for a user based on daily check-in data.
    ---
    tags:
      - Health Metrics
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            sleep_hours:
              type: number
              example: 7.5
              description: Total sleep hours (0-24)
            sleep_quality:
              type: integer
              example: 8
              description: Sleep quality score (1-10)
            energy_level:
              type: integer
              example: 7
              description: Energy level (1-10)
            morning_mood:
              type: integer
              example: 4
              description: Morning mood (1-5)
            evening_mood:
              type: integer
              example: 3
              description: Evening mood (1-5)
            stress_level:
              type: integer
              example: 4
              description: Stress level (1-10)
            did_exercise:
              type: boolean
              example: true
            ate_healthy:
              type: boolean
              example: true
            had_social_interaction:
              type: boolean
              example: false
            deep_sleep_minutes:
              type: integer
              example: 90
            rem_sleep_minutes:
              type: integer
              example: 80
            total_sleep_minutes:
              type: integer
              example: 450
            felt_rested:
              type: boolean
              example: true
    responses:
      200:
        description: Three health metrics calculated
        schema:
          type: object
          properties:
            status:
              type: string
              example: success
            m_rest:
              type: number
              example: 78.5
            m_ready:
              type: number
              example: 72.3
            m_balance:
              type: number
              example: 65.1
            overall:
              type: number
              example: 71.9
            m_rest_label:
              type: string
              example: Good
            m_ready_label:
              type: string
              example: Good
            m_balance_label:
              type: string
              example: Good
            overall_label:
              type: string
              example: Good
      400:
        description: No data provided
      500:
        description: Internal server error
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        m_rest = calc_m_rest(data)
        m_ready = calc_m_ready(data, m_rest)
        m_balance = calc_m_balance(data)
        overall = (m_rest + m_ready + m_balance) / 3.0

        result = {
            'status': 'success',
            'm_rest': round(m_rest, 1),
            'm_ready': round(m_ready, 1),
            'm_balance': round(m_balance, 1),
            'overall': round(overall, 1),
            'm_rest_label': score_label(m_rest),
            'm_ready_label': score_label(m_ready),
            'm_balance_label': score_label(m_balance),
            'overall_label': score_label(overall),
            'timestamp': datetime.now(timezone.utc).isoformat()
        }

        return Response(
            json.dumps(result, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"Error in /health-metrics/calculate: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/health-metrics/trend', methods=['POST'])
def health_metrics_trend():
    """
    Analyze trend of M-Rest/M-Ready/M-Balance across multiple days.
    ---
    tags:
      - Health Metrics
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            history:
              type: array
              description: List of daily records (newest first)
              items:
                type: object
                properties:
                  date:
                    type: string
                    example: "2025-05-10"
                  sleep_hours:
                    type: number
                  sleep_quality:
                    type: integer
                  energy_level:
                    type: integer
                  morning_mood:
                    type: integer
                  evening_mood:
                    type: integer
                  stress_level:
                    type: integer
                  did_exercise:
                    type: boolean
                  ate_healthy:
                    type: boolean
                  had_social_interaction:
                    type: boolean
    responses:
      200:
        description: Trend analysis with averages and direction
        schema:
          type: object
          properties:
            status:
              type: string
            avg_m_rest:
              type: number
            avg_m_ready:
              type: number
            avg_m_balance:
              type: number
            avg_overall:
              type: number
            m_rest_trend:
              type: string
              description: improving / declining / stable
            m_ready_trend:
              type: string
            m_balance_trend:
              type: string
            daily_scores:
              type: array
      400:
        description: No data provided or empty history
    """
    try:
        body = request.get_json()
        if not body or not body.get('history'):
            return jsonify({'error': 'No history data provided'}), 400

        history = body['history']
        daily_scores = []

        for record in history:
            mr = calc_m_rest(record)
            mready = calc_m_ready(record, mr)
            mb = calc_m_balance(record)
            overall = (mr + mready + mb) / 3.0
            daily_scores.append({
                'date': record.get('date', ''),
                'm_rest': round(mr, 1),
                'm_ready': round(mready, 1),
                'm_balance': round(mb, 1),
                'overall': round(overall, 1)
            })

        def trend_direction(scores_list):
            # history is newest-first, so scores_list[:mid] = recent, scores_list[mid:] = older
            # improving = recent scores are higher than older scores
            if len(scores_list) < 2:
                return 'stable'
            mid = len(scores_list) // 2
            recent_avg = np.mean(scores_list[:mid])
            older_avg  = np.mean(scores_list[mid:])
            diff = recent_avg - older_avg  # positive = recent better = improving
            if abs(diff) < 3.0:
                return 'stable'
            return 'improving' if diff > 0 else 'declining'

        m_rest_vals = [d['m_rest'] for d in daily_scores]
        m_ready_vals = [d['m_ready'] for d in daily_scores]
        m_balance_vals = [d['m_balance'] for d in daily_scores]
        overall_vals = [d['overall'] for d in daily_scores]

        result = {
            'status': 'success',
            'days_analyzed': len(daily_scores),
            'avg_m_rest': round(float(np.mean(m_rest_vals)), 1) if m_rest_vals else 0,
            'avg_m_ready': round(float(np.mean(m_ready_vals)), 1) if m_ready_vals else 0,
            'avg_m_balance': round(float(np.mean(m_balance_vals)), 1) if m_balance_vals else 0,
            'avg_overall': round(float(np.mean(overall_vals)), 1) if overall_vals else 0,
            'm_rest_trend': trend_direction(m_rest_vals),
            'm_ready_trend': trend_direction(m_ready_vals),
            'm_balance_trend': trend_direction(m_balance_vals),
            'daily_scores': daily_scores
        }

        return Response(
            json.dumps(convert_to_serializable(result), ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"Error in /health-metrics/trend: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


@app.route('/health-metrics/ml-predict', methods=['POST'])
def health_metrics_ml_predict():
    """
    Predict M-Rest, M-Ready, M-Balance using trained XGBoost models.
    Falls back to formula calculation when models are not yet trained.
    ---
    tags:
      - Health Metrics
    consumes:
      - application/json
    parameters:
      - in: body
        name: body
        required: true
        schema:
          type: object
          properties:
            sleep_hours:
              type: number
              example: 7.5
            sleep_quality:
              type: integer
              example: 8
            energy_level:
              type: integer
              example: 7
            morning_mood:
              type: integer
              example: 4
            evening_mood:
              type: integer
              example: 3
            stress_level:
              type: number
              example: 4.0
            did_exercise:
              type: boolean
              example: true
            ate_healthy:
              type: boolean
              example: true
            had_social_interaction:
              type: boolean
              example: false
            felt_rested:
              type: boolean
              example: true
            deep_sleep_minutes:
              type: number
              example: 90.0
            rem_sleep_minutes:
              type: number
              example: 80.0
            total_sleep_minutes:
              type: number
              example: 450.0
    responses:
      200:
        description: ML-predicted health metrics with model info
        schema:
          type: object
          properties:
            status:
              type: string
              example: success
            source:
              type: string
              example: "xgboost_model"
              description: "'xgboost_model' or 'formula_fallback'"
            m_rest:
              type: number
              example: 78.5
            m_ready:
              type: number
              example: 72.3
            m_balance:
              type: number
              example: 65.1
            overall:
              type: number
              example: 71.9
      400:
        description: No data provided
      500:
        description: Internal server error
    """
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400

        source = 'formula_fallback'
        if _hm_models:
            # Build feature vector
            sleep_hours = float(data.get('sleep_hours', 7.0))
            total_min   = float(data.get('total_sleep_minutes') or sleep_hours * 60)
            deep_min    = float(data.get('deep_sleep_minutes') or total_min * 0.20)
            rem_min     = float(data.get('rem_sleep_minutes')  or total_min * 0.22)
            sleep_eff   = (deep_min + rem_min) / total_min if total_min > 0 else 0.42
            energy      = float(data.get('energy_level', 5))
            morning_m   = float(data.get('morning_mood', 3))
            stress      = float(data.get('stress_level', 5))

            row = [
                sleep_hours,
                float(data.get('sleep_quality', 5)),
                energy,
                morning_m,
                float(data.get('evening_mood', 3)),
                stress,
                1.0 if data.get('did_exercise') else 0.0,
                1.0 if data.get('ate_healthy') else 0.0,
                1.0 if data.get('had_social_interaction') else 0.0,
                1.0 if data.get('felt_rested') else 0.0,
                deep_min,
                rem_min,
                total_min,
                max(0.0, 7.0 - sleep_hours),        # sleep_debt
                sleep_eff,                            # sleep_efficiency
                (energy + morning_m * 2 + (10 - stress)) / 5,  # wellbeing_index
                sum([
                    1 if data.get('did_exercise') else 0,
                    1 if data.get('ate_healthy') else 0,
                    1 if data.get('had_social_interaction') else 0,
                ]),                                   # habit_count
            ]
            import numpy as _np
            X = _np.array([row])
            preds = {}
            for t in ['m_rest', 'm_ready', 'm_balance']:
                X_s = _hm_scalers[t].transform(X)
                preds[t] = float(_np.clip(_hm_models[t].predict(X_s)[0], 0, 100))
            m_rest, m_ready, m_balance = preds['m_rest'], preds['m_ready'], preds['m_balance']
            source = 'xgboost_model'
        else:
            m_rest    = calc_m_rest(data)
            m_ready   = calc_m_ready(data, m_rest)
            m_balance = calc_m_balance(data)

        overall = (m_rest + m_ready + m_balance) / 3.0
        result = {
            'status':         'success',
            'source':         source,
            'm_rest':         round(m_rest,    1),
            'm_ready':        round(m_ready,   1),
            'm_balance':      round(m_balance, 1),
            'overall':        round(overall,   1),
            'm_rest_label':   score_label(m_rest),
            'm_ready_label':  score_label(m_ready),
            'm_balance_label': score_label(m_balance),
            'overall_label':  score_label(overall),
            'timestamp':      datetime.now(timezone.utc).isoformat()
        }
        return Response(
            json.dumps(result, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"Error in /health-metrics/ml-predict: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=False)
