# Используем 3.11, так как pandas 2.2+ и scikit-learn 1.5+ требуют Python >= 3.10
FROM python:3.11-slim

WORKDIR /app

# Системные зависимости для компиляции CatBoost/XGBoost и работы OpenMP
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Сначала копируем зависимости
COPY requirements.txt .

# Обновляем pip и ставим пакеты
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Копируем исходный код сервиса
COPY ml_service.py .

# Копируем модели (убедись, что папка существует локально)
COPY notebooks/models/ ./notebooks/models/

# Открываем порт для Flask
EXPOSE 5001

# Проверка статуса сервиса внутри контейнера
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD python -c "import requests; requests.get('http://localhost:5001/health')" || exit 1

# Запуск
CMD ["python", "ml_service.py"]