FROM python:3.10-slim

LABEL maintainer="opensnail"
LABEL app-name="snail-ai-paddle-ocr-api"
LABEL description="PaddleOCR HTTP service for Snail AI document image OCR enhancement."

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

ARG DEBIAN_MIRROR=https://deb.debian.org

RUN sed -i "s|http://deb.debian.org|${DEBIAN_MIRROR}|g" /etc/apt/sources.list.d/debian.sources \
    && apt-get -o Acquire::Retries=5 update \
    && apt-get install -y --no-install-recommends \
        curl \
        libgl1 \
        libglib2.0-0 \
        libgomp1 \
    && rm -rf /var/lib/apt/lists/*

ARG PIP_INDEX_URL=https://pypi.org/simple
ENV PIP_NO_CACHE_DIR=1 \
    PIP_DEFAULT_TIMEOUT=120 \
    PIP_RETRIES=10 \
    PIP_INDEX_URL=${PIP_INDEX_URL}

COPY requirements.txt .

ARG PADDLEPADDLE_VERSION=3.2.2
ARG PADDLEPADDLE_PACKAGE=paddlepaddle
ARG PADDLEPADDLE_INDEX=https://www.paddlepaddle.org.cn/packages/stable/cpu/

RUN python -m pip install --retries 10 --timeout 120 --upgrade pip \
    && python -m pip install --retries 10 --timeout 120 "${PADDLEPADDLE_PACKAGE}==${PADDLEPADDLE_VERSION}" -i "${PADDLEPADDLE_INDEX}" \
    && python -m pip install --retries 10 --timeout 120 -r requirements.txt

COPY main.py .

ENV \
    FLAGS_use_mkldnn=0 \
    OMP_NUM_THREADS=1 \
    OPENBLAS_NUM_THREADS=1 \
    MKL_NUM_THREADS=1 \
    PADDLE_PDX_DISABLE_MODEL_SOURCE_CHECK=True \
    PADDLE_OCR_HOST=0.0.0.0 \
    PADDLE_OCR_PORT=8866 \
    PADDLE_OCR_CPU_THREADS=1

EXPOSE 8866

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8866/health || exit 1

CMD ["sh", "-c", "exec uvicorn main:app --host \"${PADDLE_OCR_HOST}\" --port \"${PADDLE_OCR_PORT}\""]
