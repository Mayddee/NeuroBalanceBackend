

# """
# Neural Balance ML Service - HYBRID Production API
# Version: 3.0.0 (ML Scores + Rule-Based Text)

# """
# import json
# from flask import Flask, request, jsonify, Response
# from flask_cors import CORS
# import numpy as np
# import pandas as pd
# import joblib
# import os
# import logging
# from datetime import datetime



# # ═══════════════════════════════════════════════════════════
# # MODEL CLASS (MUST MATCH TRAINING!)
# # ═══════════════════════════════════════════════════════════

# class HybridRecommendationModel:
#     """Гибридная модель с ML + правилами"""
#     def __init__(self):
#         self.model = None
#         self.feature_columns = []
#         self.rule_base = self._build_rule_base()
    
#     def _build_rule_base(self):
#         return {
#             'SLEEP_INCREASE': {
#                 'title': '🌙 Оптимизация сна',
#                 'scientific_basis': 'Walker (2017): Сон консолидирует память',
#                 'critical_message': 'Ваш сон критично низкий. Это главный фактор, снижающий продуктивность.',
#                 'normal_message': 'Дополнительный час сна улучшит концентрацию.',
#                 'actions': {
#                     0.5: ['Лягте сегодня на 30 минут раньше'],
#                     1.0: ['Установите будильник на отбой в 23:00', 'Уберите телефон из спальни'],
#                     1.5: ['Создайте вечерний ритуал', 'Избегайте кофеина после 15:00'],
#                     2.0: ['Спите 8 часов (23:00-7:00)', 'Затемните комнату', 'Температура 18-20°C']
#                 }
#             },
#             'STRESS_DECREASE': {
#                 'title': '🧘 Управление стрессом',
#                 'scientific_basis': 'McEwen (2007): Стресс повреждает гиппокамп',
#                 'critical_message': 'Стресс критический. Это блокирует ясное мышление.',
#                 'normal_message': 'Снижение стресса повысит умственную гибкость.',
#                 'actions': {
#                     -1: ['3 глубоких вдоха (4 сек вдох, 6 сек выдох)'],
#                     -2: ['10-минутная медитация утром', 'Прогулка 15 мин'],
#                     -3: ['Техника Pomodoro (25/5)', 'Делегируйте 2 задачи'],
#                     -4: ['Запишитесь к психологу', 'Прогрессивная релаксация']
#                 }
#             },
#             'EXERCISE_INCREASE': {
#                 'title': '💪 Физическая активность',
#                 'scientific_basis': 'Ratey (2008): Упражнения повышают BDNF',
#                 'critical_message': 'Почти нет движения. Активность критична для когнитивных функций.',
#                 'normal_message': 'Тренировки усилят ментальную энергию.',
#                 'actions': {
#                     1: ['30-минутная прогулка 3 раза в неделю'],
#                     2: ['2 силовые тренировки по 20 мин', 'Лестница вместо лифта'],
#                     3: ['4 раза: 2 кардио + 2 силовые', 'Спортивная группа'],
#                     4: ['5-6 раз в неделю', 'Попробуйте HIIT']
#                 }
#             },
#             'SCREEN_DECREASE': {
#                 'title': '📱 Цифровое благополучие',
#                 'scientific_basis': 'Newport (2016): Минимализм восстанавливает фокус',
#                 'critical_message': 'Экранное время критично. Это истощает глубокую концентрацию.',
#                 'normal_message': 'Сокращение экранов улучшит качество внимания.',
#                 'actions': {
#                     -1: ['Лимит 1 час для соцсетей'],
#                     -2: ['Правило 20-20-20', 'Отключите уведомления'],
#                     -3: ['Ч/б режим после 20:00', 'Чтение вместо скроллинга 30 мин'],
#                     -4: ['Цифровой закат в 21:00', 'Удалите 2 отвлекающих приложения']
#                 }
#             }
#         }
    
#     def predict_improvement(self, current_state, action_type, action_magnitude):
#         baseline_map = {
#             'SLEEP_INCREASE': ('sleep_duration', 6.5, lambda x: x < 6.5),
#             'STRESS_DECREASE': ('stress_level', 7, lambda x: x >= 7),
#             'EXERCISE_INCREASE': ('exercise_frequency', 2, lambda x: x <= 2),
#             'SCREEN_DECREASE': ('daily_screen_time', 10, lambda x: x >= 10)
#         }
        
#         if action_type in baseline_map:
#             key, threshold, crit_func = baseline_map[action_type]
#             baseline_value = current_state.get(key, threshold)
#             is_critical = 1 if crit_func(baseline_value) else 0
#         else:
#             baseline_value = 0
#             is_critical = 0
        
#         features = {
#             'original_sleep': current_state.get('sleep_duration', 7),
#             'original_stress': current_state.get('stress_level', 5),
#             'original_exercise': current_state.get('exercise_frequency', 3),
#             'original_screen': current_state.get('daily_screen_time', 8),
#             'original_score': current_state.get('cognitive_score', 70),
#             'action_magnitude': action_magnitude,
#             'baseline_value': baseline_value,
#             'is_critical': is_critical,
#             'magnitude_x_baseline': abs(action_magnitude) * baseline_value
#         }
        
#         for col in self.feature_columns:
#             if col.startswith('action_'):
#                 features[col] = 1 if col == f'action_{action_type}' else 0
        
#         X_pred = pd.DataFrame([features])[self.feature_columns]
#         improvement = self.model.predict(X_pred)[0]
#         return float(improvement)
    
#     def generate_recommendation(self, current_state, action_type, action_magnitude, predicted_improvement):
#         rule = self.rule_base[action_type]
        
#         baseline_map = {
#             'SLEEP_INCREASE': current_state.get('sleep_duration', 7) < 6.5,
#             'STRESS_DECREASE': current_state.get('stress_level', 5) >= 7,
#             'EXERCISE_INCREASE': current_state.get('exercise_frequency', 3) <= 2,
#             'SCREEN_DECREASE': current_state.get('daily_screen_time', 8) >= 10
#         }
        
#         is_critical = baseline_map.get(action_type, False)
#         message = rule['critical_message'] if is_critical else rule['normal_message']
        
#         mag_key = min(rule['actions'].keys(), key=lambda x: abs(x - abs(action_magnitude)))
#         actions = rule['actions'][mag_key]
        
#         if predicted_improvement > 10:
#             priority = 'CRITICAL'
#         elif predicted_improvement > 5:
#             priority = 'HIGH'
#         elif predicted_improvement > 2:
#             priority = 'MEDIUM'
#         else:
#             priority = 'LOW'
        
#         return {
#             'action_type': action_type,
#             'title': rule['title'],
#             'message': message,
#             'predicted_improvement': round(predicted_improvement, 1),
#             'priority': priority,
#             'actions': actions,
#             'scientific_basis': rule['scientific_basis']
#         }

# # ═══════════════════════════════════════════════════════════
# # FLASK APP
# # ═══════════════════════════════════════════════════════════

# app = Flask(__name__)
# app.config['JSON_AS_ASCII'] = False
# CORS(app)

# logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
# logger = logging.getLogger(__name__)

# BASE_DIR = os.path.dirname(os.path.abspath(__file__))
# MODEL_DIR = os.path.join(BASE_DIR, 'notebooks', 'models')

# logger.info(f"Loading models from: {MODEL_DIR}")

# try:
#     regression_model = joblib.load(os.path.join(MODEL_DIR, 'cognitive_score_model.pkl'))
#     classification_model = joblib.load(os.path.join(MODEL_DIR, 'cognitive_state_model.pkl'))
#     scaler = joblib.load(os.path.join(MODEL_DIR, 'scaler.pkl'))
#     label_encoder = joblib.load(os.path.join(MODEL_DIR, 'label_encoder.pkl'))
#     hybrid_recommender = joblib.load(os.path.join(MODEL_DIR, 'ml_recommendation_model.pkl'))
    
#     with open(os.path.join(MODEL_DIR, 'feature_names.txt'), 'r') as f:
#         feature_names = [line.strip() for line in f.readlines()]
    
#     logger.info(f"✅ HYBRID system loaded! Features: {len(feature_names)}")
    
# except Exception as e:
#     logger.error(f"Failed to load models: {e}")
#     raise

# # ═══════════════════════════════════════════════════════════
# # HELPERS
# # ═══════════════════════════════════════════════════════════

# def calculate_cfi(data):
#     r_norm = (data.get('reaction_time', 350) - 200) / 400
#     s_norm = (data.get('stress_level', 5) - 1) / 9
#     sleep_debt = max(0, 7 - data.get('sleep_duration', 7)) / 3
#     screen_fat = max(0, data.get('daily_screen_time', 8) - 8) / 4
    
#     cfi = (0.30*r_norm + 0.25*s_norm + 0.25*sleep_debt + 0.20*screen_fat) * 100
    
#     if cfi < 25:
#         level = 'FRESH'
#     elif cfi < 50:
#         level = 'NORMAL'
#     elif cfi < 75:
#         level = 'TIRED'
#     else:
#         level = 'EXHAUSTED'
    
#     return {'cfi_score': round(cfi, 2), 'fatigue_level': level}

# def preprocess_user_input(data):
#     f_dict = {}
    
#     f_dict['Sleep_Duration'] = float(data.get('sleep_duration', 7.0))
#     f_dict['Stress_Level'] = float(data.get('stress_level', 5.0))
#     f_dict['Daily_Screen_Time'] = float(data.get('daily_screen_time', 8.0))
#     f_dict['Exercise_Frequency_Num'] = float(data.get('exercise_frequency', 3.0))
#     f_dict['Caffeine_Intake'] = float(data.get('caffeine_intake', 2.0))
#     f_dict['Reaction_Time'] = float(data.get('reaction_time', 350.0))
#     f_dict['Memory_Test_Score'] = float(data.get('memory_test_score', 70.0))
#     f_dict['Age'] = float(data.get('age', 30.0))
    
#     gender = str(data.get('gender', 'Male')).lower()
#     f_dict['Gender_Encoded'] = 1 if gender == 'male' else 0
    
#     diet = str(data.get('diet_type', 'Non-Vegetarian'))
#     f_dict['Diet_Non-Vegetarian'] = 1 if 'Non' in diet else 0
#     f_dict['Diet_Vegan'] = 1 if diet == 'Vegan' else 0
#     f_dict['Diet_Vegetarian'] = 1 if diet == 'Vegetarian' else 0
    
#     # CFI
#     r_norm = (f_dict['Reaction_Time'] - 200) / 400
#     s_norm = (f_dict['Stress_Level'] - 1) / 9
#     sleep_debt = max(0, 7 - f_dict['Sleep_Duration']) / 3
#     screen_fat = max(0, f_dict['Daily_Screen_Time'] - 8) / 4
#     f_dict['CFI'] = (0.30*r_norm + 0.25*s_norm + 0.25*sleep_debt + 0.20*screen_fat) * 100
    
#     f_dict['Sleep_Debt'] = max(0, 7 - f_dict['Sleep_Duration'])
#     f_dict['Memory_Efficiency'] = (f_dict['Memory_Test_Score'] / f_dict['Reaction_Time']) * 1000
#     f_dict['Sleep_Exercise_Interaction'] = f_dict['Sleep_Duration'] * f_dict['Exercise_Frequency_Num']
#     f_dict['Stress_Screen_Interaction'] = f_dict['Stress_Level'] * f_dict['Daily_Screen_Time']
    
#     # Lifestyle Balance
#     sleep_sc = np.clip((f_dict['Sleep_Duration'] - 4) / 6, 0, 1)
#     ex_sc = np.clip(f_dict['Exercise_Frequency_Num'] / 7, 0, 1)
#     st_sc = 1 - ((f_dict['Stress_Level'] - 1) / 9)
#     scr_sc = 1 - np.clip((f_dict['Daily_Screen_Time'] - 1) / 11, 0, 1)
#     f_dict['Lifestyle_Balance'] = (0.30*sleep_sc + 0.25*st_sc + 0.20*ex_sc + 0.15*scr_sc) * 100
    
#     f_dict['Sleep_Performance_Ratio'] = 70 / (f_dict['Sleep_Duration'] + 0.1)
#     f_dict['Stress_Resilience'] = 70 / f_dict['Stress_Level'] if f_dict['Stress_Level'] > 0 else 14.0
    
#     # Собираем вектор
#     features = [f_dict.get(name, 0.0) for name in feature_names]
#     return np.array(features).reshape(1, -1)

# # ═══════════════════════════════════════════════════════════
# # ENDPOINTS
# # ═══════════════════════════════════════════════════════════

# @app.route('/health', methods=['GET'])
# def health():
#     return jsonify({
#         'status': 'healthy',
#         'service': 'Neural Balance HYBRID ML Service',
#         'version': '3.0.0',
#         'timestamp': datetime.utcnow().isoformat(),
#         'models_loaded': {
#             'regression': regression_model is not None,
#             'classification': classification_model is not None,
#             'hybrid_recommender': hybrid_recommender is not None
#         }
#     }), 200

# @app.route('/predict', methods=['POST'])
# def predict():
#     try:
#         data = request.get_json()
#         if not data:
#             return jsonify({'error': 'No data'}), 400
        
#         logger.info(f"Prediction request: {data}")
        
#         cfi = calculate_cfi(data)
#         features = preprocess_user_input(data)
#         features_scaled = scaler.transform(features)
        
#         score = regression_model.predict(features_scaled)[0]
#         state_enc = classification_model.predict(features_scaled)[0]
#         state = label_encoder.inverse_transform([state_enc])[0]
        
#         prediction_data = {
#             'status': 'success',
#             'prediction': {
#                 'cognitive_score': round(float(score), 2),
#                 'cognitive_state': str(state),
#                 'fatigue_level': cfi['fatigue_level'],
#                 'cfi_score': cfi['cfi_score']
#             },
#             'timestamp': datetime.utcnow().isoformat()
#         }
        
#         # ЗАМЕНА: Вместо return jsonify(prediction_data)
#         return Response(
#             json.dumps(prediction_data, ensure_ascii=False),
#             mimetype='application/json',
#             headers={'Content-Type': 'application/json; charset=utf-8'}
#         )
        
#     except Exception as e:
#         logger.error(f"Error: {e}")
#         return jsonify({'status': 'error', 'message': str(e)}), 500

# @app.route('/recommend', methods=['POST'])
# def recommend():
#     try:
#         data = request.get_json()
#         if not data:
#             return jsonify({'error': 'No data'}), 400
        
#         logger.info(f"Recommendation request: {data}")
        
#         # Predict score
#         features = preprocess_user_input(data)
#         features_scaled = scaler.transform(features)
#         score = regression_model.predict(features_scaled)[0]
#         state_enc = classification_model.predict(features_scaled)[0]
#         state = label_encoder.inverse_transform([state_enc])[0]
        
#         current_state = {
#             'sleep_duration': data.get('sleep_duration', 7),
#             'stress_level': data.get('stress_level', 5),
#             'exercise_frequency': data.get('exercise_frequency', 3),
#             'daily_screen_time': data.get('daily_screen_time', 8),
#             'cognitive_score': float(score)
#         }
        
#         # Generate HYBRID recommendations
#         recs = []
#         action_space = {
#             'SLEEP_INCREASE': [1.0, 1.5, 2.0, 2.5],
#             'STRESS_DECREASE': [-2, -3, -4],
#             'EXERCISE_INCREASE': [2, 3, 4],
#             'SCREEN_DECREASE': [-2, -3, -4]
#         }
        
#         for action_type, mags in action_space.items():
#             for mag in mags:
#                 try:
#                     improvement = hybrid_recommender.predict_improvement(current_state, action_type, mag)
#                     rec = hybrid_recommender.generate_recommendation(current_state, action_type, mag, improvement)
#                     recs.append(rec)
#                 except Exception as e:
#                     logger.warning(f"Skipping {action_type}: {e}")
        
#         # Sort
#         recs.sort(key=lambda x: x['predicted_improvement'], reverse=True)
#         top_5 = recs[:5]
        
#         total = sum(r['predicted_improvement'] for r in top_5[:3])
        
#         if state == 'HIGH':
#             summary = f"Ваша когнитивная производительность ВЫСОКАЯ ({score:.1f}/100). Продолжайте в том же духе!"
#         elif state == 'MEDIUM':
#             summary = f"Ваша когнитивная производительность СРЕДНЯЯ ({score:.1f}/100). Топ-3 действия дадут +{total:.1f} баллов."
#         else:
#             summary = f"Ваша когнитивная производительность НИЗКАЯ ({score:.1f}/100). Внедрите рекомендации для улучшения на +{total:.1f} баллов."
        
#         response_data = {
#             'status': 'success',
#             'cognitive_score': round(float(score), 1),
#             'cognitive_state': str(state),
#             'summary': summary,
#             'recommendations': top_5,
#             'total_potential_improvement': round(total, 1),
#             'timestamp': datetime.utcnow().isoformat()
#         }
        
#         # ЗАМЕНА: Вместо return jsonify(response_data)
#         return Response(
#             json.dumps(response_data, ensure_ascii=False),
#             mimetype='application/json',
#             headers={'Content-Type': 'application/json; charset=utf-8'}
#         )
        
#     except Exception as e:
#         logger.error(f"Error: {e}")
#         return jsonify({'status': 'error', 'message': str(e)}), 500

# if __name__ == '__main__':
#     port = int(os.environ.get('PORT', 5001))
#     logger.info(f"Starting HYBRID ML Service on port {port}...")
#     app.run(host='0.0.0.0', port=port, debug=False)

"""
Neural Balance ML Service - ULTIMATE Edition
Version: 5.0.0
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