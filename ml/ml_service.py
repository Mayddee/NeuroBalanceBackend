
"""
Neural Balance ML Service - ULTIMATE Edition
Version: 5.0.1
"""

from flask import Flask, request, jsonify, Response
from flask_cors import CORS
import numpy as np
import pandas as pd
import joblib
from catboost import CatBoostRegressor
import logging
from datetime import datetime
from pathlib import Path
import json

def convert_to_serializable(obj):
    """Превращает numpy-типы в стандартные типы Python для JSON"""
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
# 1. ОБЯЗАТЕЛЬНЫЕ КЛАССЫ (Код из ноутбука)
# ═══════════════════════════════════════════════════════════

class SmartRecommendationSelector:
    def __init__(self, catboost_model, xgb_regression_model, feature_names):
        self.catboost = catboost_model
        self.xgb = xgb_regression_model
        self.features = feature_names  # FINAL_FEATURES
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
            if best_rec: all_recommendations.append(best_rec)
        
        all_recommendations.sort(key=lambda x: x['predicted_improvement'], reverse=True)
        top_3 = all_recommendations[:3]
        
        return {
            'cognitive_score': round(current_score, 1),
            'recommendations': top_3,
            'total_potential': round(sum(r['predicted_improvement'] for r in top_3), 1),
            'summary': f"Ваш когнитивный счет: {current_score:.1f}. Рекомендации могут дать +{sum(r['predicted_improvement'] for r in top_3):.1f} балла."
        }
    
    def _find_best_for_type(self, action_type, user_data, current_score):
        rule = self.rules[action_type]
        deltas = list(rule['actions'].keys())
        param_map = {'SLEEP_INCREASE': 'sleep_duration', 'STRESS_DECREASE': 'stress_level', 
                     'EXERCISE_INCREASE': 'exercise_frequency_num', 'SCREEN_DECREASE': 'daily_screen_time'}
        baseline = user_data.get(param_map[action_type], 5)
        candidates = []
        for d in deltas:
            new_v = np.clip(baseline + d, 0, 16)
            imp = self._predict_impact(user_data, action_type, d, baseline, new_v, current_score)
            candidates.append({'delta': d, 'new_v': new_v, 'imp': imp})
        best = max(candidates, key=lambda x: x['imp'])
        return {
            'type': action_type, 'title': rule['title'], 'predicted_improvement': round(best['imp'], 1),
            'priority': 'CRITICAL' if best['imp'] > 10 else ('HIGH' if best['imp'] > 6 else 'MEDIUM'),
            'actions': rule['actions'][best['delta']], 'scientific_basis': rule['basis'], 
            'baseline': round(baseline, 1), 'recommended_target': round(best['new_v'], 1)
        }

    def _predict_impact(self, user_data, action_type, delta, baseline, new_value, current_score):
        row = {k.lower(): v for k, v in user_data.items()}
        row.update({'action_delta': float(delta), 'baseline_value': float(baseline), 
                    'new_value': float(new_value), 'baseline_score': float(current_score),
                    'is_critical': 1.0 if self._check_critical(action_type, baseline) else 0.0,
                    'is_optimal_zone': 1.0 if self._check_optimal(action_type, new_value) else 0.0})
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
# 2. SETUP & LOADING
# ═══════════════════════════════════════════════════════════

app = Flask(__name__)
app.config['JSON_AS_ASCII'] = False
CORS(app)
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).parent
MODEL_DIR = BASE_DIR / 'notebooks' / 'models'

try:
    xgb_reg = joblib.load(MODEL_DIR / 'cognitive_score_model.pkl')
    scaler = joblib.load(MODEL_DIR / 'scaler.pkl')
    label_encoder = joblib.load(MODEL_DIR / 'label_encoder.pkl')
    
    # Загружаем CatBoost напрямую
    catboost_model = CatBoostRegressor()
    catboost_model.load_model(str(MODEL_DIR / 'catboost_recommendation_model.cbm'))
    
    # Пытаемся загрузить smart_selector. Если не выйдет - инициализируем вручную.
    with open(MODEL_DIR / 'feature_names.txt', 'r') as f:
        FINAL_FEATURES = [line.strip() for line in f.readlines()]
    
    smart_selector = SmartRecommendationSelector(catboost_model, xgb_reg, FINAL_FEATURES)
    
    logger.info("✅ All models and classes loaded successfully!")
except Exception as e:
    logger.error(f"❌ Initialization error: {e}")
    raise

# ═══════════════════════════════════════════════════════════
# 3. HELPERS
# ═══════════════════════════════════════════════════════════

def calc_engineered_features(data):
    res = data.copy()
    r_norm = (data.get('reaction_time', 350) - 200) / 400
    s_norm = (data.get('stress_level', 5) - 1) / 9
    sleep_debt = max(0, 7 - data.get('sleep_duration', 7)) / 3
    screen_fat = max(0, data.get('daily_screen_time', 8) - 8) / 4
    res['cfi'] = (0.30*r_norm + 0.25*s_norm + 0.25*sleep_debt + 0.20*screen_fat) * 100
    res['sleep_debt'] = max(0, 7 - data.get('sleep_duration', 7))
    res['memory_efficiency'] = (data.get('memory_test_score', 70) / data.get('reaction_time', 350)) * 1000
    sleep_sc = np.clip((data.get('sleep_duration', 7) - 4) / 6, 0, 1)
    ex_sc = np.clip(data.get('exercise_frequency_num', 3) / 7, 0, 1)
    st_sc = 1 - ((data.get('stress_level', 5) - 1) / 9)
    scr_sc = 1 - np.clip((data.get('daily_screen_time', 8) - 1) / 11, 0, 1)
    res['lifestyle_balance'] = (0.30*sleep_sc + 0.25*st_sc + 0.20*ex_sc + 0.15*scr_sc) * 100
    res['sleep_exercise_interaction'] = data.get('sleep_duration', 7) * data.get('exercise_frequency_num', 3)
    res['exercise_frequency_num'] = data.get('exercise_frequency', 3)
    return res

# ═══════════════════════════════════════════════════════════
# 4. ENDPOINTS
# ═══════════════════════════════════════════════════════════

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'healthy', 'version': '5.0.0'}), 200

@app.route('/recommend/top3', methods=['POST'])
def recommend_top3():
    try:
        data = request.get_json()
        user_data = calc_engineered_features(data)
        
        # Получаем ТОП-3
        result = smart_selector.select_top_3(user_data, scaler)
        
        # ОЧЕНЬ ВАЖНО: Конвертируем все numpy.int64/float64 в обычные типы
        clean_result = convert_to_serializable(result)
        
        return Response(
            json.dumps({'status': 'success', **clean_result}, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"❌ Error: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500
    
@app.route('/predict', methods=['POST'])
def predict():
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'No data provided'}), 400
        
        # 1. Расчет признаков
        user_data = calc_engineered_features(data)
        
        # 2. Подготовка вектора для XGBoost (текущий счет)
        # Важно: используем порядок из FINAL_FEATURES
        user_df = pd.DataFrame([user_data]).reindex(columns=smart_selector.features, fill_value=0)
        vec_scaled = scaler.transform(user_df)
        
        # 3. Предсказание
        score = float(xgb_reg.predict(vec_scaled)[0])
        
        # 4. Определение состояния через LabelEncoder (если он есть)
        # Если классификатор не загружен, можно просто по порогам:
        state = "Unknown"
        if score > 75: state = "High"
        elif score > 50: state = "Medium"
        else: state = "Low"

        response_data = {
            'status': 'success',
            'cognitive_score': round(score, 1),
            'cognitive_state': state,
            'cfi_score': round(user_data.get('cfi', 0), 1),
            'timestamp': datetime.utcnow().isoformat()
        }
        
        return Response(
            json.dumps(response_data, ensure_ascii=False),
            mimetype='application/json',
            headers={'Content-Type': 'application/json; charset=utf-8'}
        )
    except Exception as e:
        logger.error(f"❌ Error in /predict: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=False)