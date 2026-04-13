

class CognitiveRecommendationEngine:
    """
    Генератор персональных практических рекомендаций
    
    Научное обоснование:
    - Гигиена сна (Walker, 2017)
    - Управление стрессом (McEwen, 2007)
    - Нейробиология физических нагрузок (Ratey, 2008)
    - Цифровое благополучие (Newport, 2016)
    """
    
    def __init__(self):
        self.recommendation_db = self._build_recommendation_database()
    
    def _build_recommendation_database(self):
        """
        База данных научно обоснованных шаблонов рекомендаций
        """
        return {
            'sleep': {
                'critical': {
                    'condition': lambda x: x.get('sleep_duration', 8) < 6,
                    'message': "🚨 КРИТИЧЕСКИЙ НЕДОСЫП: Вы спите менее 6 часов. Это серьезно ухудшает когнитивные функции. "
                              "Цель: 7-9 часов для оптимальной работы мозга.",
                    'impact': "+25% к когнитивному счету",
                    'priority': 'CRITICAL',
                    'actions': [
                        "Установите постоянное время отхода ко сну (в одно и то же время)",
                        "Создайте ритуал расслабления (за 30 мин до сна)",
                        "Избегайте экранов за 1 час до сна",
                        "Поддерживайте прохладу в спальне (18-20°C)"
                    ]
                },
                'suboptimal': {
                    'condition': lambda x: 6 <= x.get('sleep_duration', 8) < 7,
                    'message': "⚠️ НЕДОСТАТОЧНО СНА: Вы спите 6-7 часов. Увеличение до 7-8 часов улучшит концентрацию и память.",
                    'impact': "+15% к когнитивному счету",
                    'priority': 'HIGH',
                    'actions': [
                        "Ложитесь спать на 30 минут раньше",
                        "Сократите потребление кофеина после 14:00"
                    ]
                },
                'optimal': {
                    'condition': lambda x: 7 <= x.get('sleep_duration', 8) <= 9,
                    'message': "✅ Отличная продолжительность сна! Поддерживайте этот режим.",
                    'impact': "Оптимально",
                    'priority': 'LOW'
                }
            },
            'stress': {
                'critical': {
                    'condition': lambda x: x.get('stress_level', 0) >= 8,
                    'message': "🚨 ОПАСНЫЙ УРОВЕНЬ СТРЕССА: Высокий стресс разрушает память и способность принимать решения. "
                              "Требуются немедленные действия.",
                    'impact': "Снижение риска падения когнитивного счета на 35%",
                    'priority': 'CRITICAL',
                    'actions': [
                        "Выполните 10-минутную дыхательную практику (техника 4-7-8)",
                        "Сделайте 15-минутную прогулку на свежем воздухе",
                        "Рассмотрите возможность работы со специалистом (психологом)",
                        "Делегируйте или отложите несрочные задачи"
                    ]
                },
                'elevated': {
                    'condition': lambda x: 5 <= x.get('stress_level', 0) < 8,
                    'message': "⚠️ ПОВЫШЕННЫЙ СТРЕСС: Примените стратегии снижения стресса, чтобы избежать выгорания.",
                    'impact': "Предотвращение падения когнитивного счета на 20%",
                    'priority': 'HIGH',
                    'actions': [
                        "5 минут медитации или практики осознанности",
                        "Физическая активность (30 минут)",
                        "Разбейте крупные задачи на мелкие шаги"
                    ]
                },
                'manageable': {
                    'condition': lambda x: x.get('stress_level', 0) < 5,
                    'message': "✅ Уровень стресса в норме.",
                    'priority': 'LOW'
                }
            },
            'exercise': {
                'insufficient': {
                    'condition': lambda x: x.get('exercise_frequency', 3) < 2,
                    'message': "⚠️ МАЛО АКТИВНОСТИ: Низкая физическая нагрузка. Активность повышает BDNF (фактор роста мозга).",
                    'impact': "Потенциал роста когнитивного счета на +20%",
                    'priority': 'MEDIUM',
                    'actions': [
                        "Начните с 3 прогулок по 30 минут в неделю",
                        "Добавьте силовые тренировки 2 раза в неделю",
                        "Делайте короткие разминки (встать/походить) каждый час"
                    ]
                },
                'moderate': {
                    'condition': lambda x: 2 <= x.get('exercise_frequency', 3) < 4,
                    'message': "👍 Хороший уровень активности. Рассмотрите увеличение до 4-5 тренировок для максимальной пользы.",
                    'priority': 'LOW'
                },
                'optimal': {
                    'condition': lambda x: x.get('exercise_frequency', 3) >= 4,
                    'message': "✅ Отличный режим тренировок!",
                    'priority': 'LOW'
                }
            },
            'screen_time': {
                'excessive': {
                    'condition': lambda x: x.get('screen_time', 0) > 10,
                    'message': "🚨 ИЗБЫТОК ЭКРАННОГО ВРЕМЕНИ: >10 часов вызывают фрагментацию внимания и сильное напряжение глаз.",
                    'impact': "Улучшение способности фокусироваться на 25%",
                    'priority': 'HIGH',
                    'actions': [
                        "Немедленно сократите использование экранов на 2 часа",
                        "Используйте правило 20-20-20 (каждые 20 мин смотреть на 20 футов вдаль в течение 20 сек)",
                        "Включите фильтры синего света на всех устройствах",
                        "Установите лимиты времени для развлекательных приложений"
                    ]
                },
                'high': {
                    'condition': lambda x: 8 <= x.get('screen_time', 0) <= 10,
                    'message': "⚠️ ВЫСОКОЕ ЭКРАННОЕ ВРЕМЯ: Сократите его для улучшения концентрации.",
                    'priority': 'MEDIUM',
                    'actions': [
                        "Делайте перерывы без экранов каждый час",
                        "Используйте приложения-трекеры для блокировки соцсетей"
                    ]
                },
                'optimal': {
                    'condition': lambda x: x.get('screen_time', 0) < 8,
                    'message': "✅ Экранное время в пределах здоровой нормы.",
                    'priority': 'LOW'
                }
            },
            'caffeine': {
                'excessive': {
                    'condition': lambda x: x.get('caffeine_intake', 0) > 4,
                    'message': "⚠️ ИЗБЫТОК КОФЕИНА: Высокое потребление (>400 мг) может вызвать тревожность и нарушить сон.",
                    'priority': 'MEDIUM',
                    'actions': [
                        "Сократите до 2-3 чашек в день",
                        "Полностью исключите кофеин после 14:00",
                        "Попробуйте заменить кофе на зеленый чай"
                    ]
                },
                'none': {
                    'condition': lambda x: x.get('caffeine_intake', -1) == 0,
                    'message': "ℹ️ Умеренное потребление кофеина (1-2 чашки) может улучшить фокус, если пить его в первой половине дня.",
                    'priority': 'LOW'
                },
                'optimal': {
                    'condition': lambda x: 1 <= x.get('caffeine_intake', 0) <= 3,
                    'message': "✅ Умеренное потребление кофеина.",
                    'priority': 'LOW'
                }
            },
            'fatigue': {
                'exhausted': {
                    'condition': lambda x: x.get('cfi', 0) > 75,
                    'message': "🚨 СИЛЬНОЕ КОГНИТИВНОЕ ИСТОЩЕНИЕ: Вам нужен немедленный отдых.",
                    'priority': 'CRITICAL',
                    'actions': [
                        "НЕМЕДЛЕННО остановите выполнение любых сложных задач",
                        "Организуйте 20-минутный дневной сон (power nap)",
                        "Перенесите принятие важных решений на завтра",
                        "Обеспечьте себе не менее 8 часов сна сегодня ночью"
                    ]
                },
                'tired': {
                    'condition': lambda x: 50 <= x.get('cfi', 0) <= 75,
                    'message': "⚠️ ЗНАЧИТЕЛЬНАЯ УСТАЛОСТЬ: Снизьте текущую когнитивную нагрузку.",
                    'priority': 'HIGH',
                    'actions': [
                        "Сфокусируйтесь на простых и рутинных задачах",
                        "Делайте частые перерывы (по 5 минут каждые 25 минут работы)",
                        "Строго избегайте многозадачности"
                    ]
                },
                'fresh': {
                    'condition': lambda x: x.get('cfi', 0) < 25,
                    'message': "✅ Пик когнитивной формы! Беритесь за самые сложные задачи прямо сейчас.",
                    'priority': 'LOW'
                }
            }
        }
    
    def generate_recommendations(self, user_data, predicted_score, predicted_state):
        """
        Генерация персональных рекомендаций
        
        Args:
            user_data: словарь с метриками пользователя
            predicted_score: предсказанный когнитивный балл
            predicted_state: предсказанный когнитивный статус (LOW/MED/HIGH)
        
        Returns:
            Словарь со счетом, статусом, резюме и отсортированным списком рекомендаций
        """
        recommendations = []
        
        # Создаем копию, чтобы не менять исходный словарь
        # process_data = user_data.copy()
        process_data = {k.lower(): v for k, v in user_data.items()}
        process_data['predicted_score'] = predicted_score
        process_data['predicted_state'] = predicted_state
        
        # Проверка каждой категории
        for category, rules in self.recommendation_db.items():
            for rule_name, rule in rules.items():
                if rule['condition'](process_data):
                    rec = {
                        'category': category.upper(),
                        'severity': rule_name.upper(),
                        'priority': rule['priority'],
                        'message': rule['message'],
                        'impact': rule.get('impact', ''),
                        'actions': rule.get('actions', [])
                    }
                    recommendations.append(rec)
                    break  # Только одно правило (состояние) на категорию
        
        # Сортировка по приоритету (от критических к незначительным)
        priority_order = {'CRITICAL': 0, 'HIGH': 1, 'MEDIUM': 2, 'LOW': 3}
        recommendations.sort(key=lambda x: priority_order.get(x['priority'], 3))
        
        # Передаем список рекомендаций для правильного подсчета критических факторов
        summary = self._generate_summary(recommendations, predicted_score, predicted_state)
        
        return {
            'cognitive_score': round(predicted_score, 2),
            'cognitive_state': predicted_state,
            'summary': summary,
            'recommendations': recommendations,
            'total_recommendations': len([r for r in recommendations if r['priority'] != 'LOW'])
        }
    
    def _generate_summary(self, recommendations, predicted_score, predicted_state):
        """
        Генерация общего вывода/резюме
        """
        if predicted_state == 'HIGH' or predicted_score > 75:
            return (
                f"🎯 Ваш когнитивный статус: ВЫСОКИЙ ({predicted_score:.1f}/100). "
                f"Вы работаете на пике возможностей! Поддерживайте текущие полезные привычки."
            )
        elif predicted_state == 'MEDIUM' or 45 <= predicted_score <= 75:
            return (
                f"📊 Ваш когнитивный статус: СРЕДНИЙ ({predicted_score:.1f}/100). "
                f"У вас есть потенциал для роста. Обратите внимание на рекомендации с высоким приоритетом."
            )
        else:  # LOW
            # Считаем реальное количество критических факторов
            critical_count = sum(1 for r in recommendations if r['priority'] == 'CRITICAL')
            return (
                f"⚠️ Ваш когнитивный статус: НИЗКИЙ ({predicted_score:.1f}/100). "
                f"Сразу несколько факторов негативно влияют на вашу продуктивность. "
                f"Необходимо срочно исправить {critical_count} КРИТИЧЕСКИХ показателя(ей)."
            )
