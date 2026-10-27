# Reference image for app_fastapi.py -- not built by this course. Build/run:
#   docker build -t fraud-api .
#   docker run -p 8080:8080 -e MODEL_VERSION=v4 fraud-api
FROM python:3.11-slim

# don't run as root inside the container
RUN useradd --create-home appuser
WORKDIR /app

# install dependencies first: this layer is cached until requirements change
COPY requirements-serving.txt .
RUN pip install --no-cache-dir -r requirements-serving.txt

# the model artefact is baked in (or pulled from the registry at startup)
COPY app_fastapi.py model.joblib ./

USER appuser
ENV MODEL_PATH=/app/model.joblib PORT=8080
EXPOSE 8080
HEALTHCHECK CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/health')"
CMD ["sh", "-c", "uvicorn app_fastapi:app --host 0.0.0.0 --port ${PORT} --workers 2"]
