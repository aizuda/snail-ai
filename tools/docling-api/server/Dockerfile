FROM python:3.10-slim

LABEL maintainer="opensnail"
LABEL app-name="snail-ai-docling-api"
LABEL description="Docling REST service for Snail AI document conversion."

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    DOCLING_HOST=0.0.0.0 \
    DOCLING_PORT=5100 \
    DOCLING_MODEL_DIR=/models/docling \
    DOCLING_DOWNLOAD_MODELS=false

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
ARG PYTORCH_CPU_WHEEL_BASE=https://mirrors.aliyun.com/pytorch-wheels/cpu
ARG TARGETARCH=amd64
ENV PIP_NO_CACHE_DIR=1 \
    PIP_DEFAULT_TIMEOUT=120 \
    PIP_RETRIES=10 \
    PIP_INDEX_URL=${PIP_INDEX_URL}

COPY requirements.txt .

RUN python -m pip install --retries 10 --timeout 120 --upgrade pip \
    && if [ "${TARGETARCH}" = "amd64" ]; then \
        python -m pip install --retries 10 --timeout 120 \
            "${PYTORCH_CPU_WHEEL_BASE}/torch-2.8.0%2Bcpu-cp310-cp310-manylinux_2_28_x86_64.whl" \
            "${PYTORCH_CPU_WHEEL_BASE}/torchvision-0.23.0%2Bcpu-cp310-cp310-manylinux_2_28_x86_64.whl"; \
    else \
        python -m pip install --retries 10 --timeout 120 "torch==2.8.0" "torchvision==0.23.0"; \
    fi \
    && python -m pip install --retries 10 --timeout 120 -r requirements.txt

COPY main.py docker-entrypoint.sh ./
RUN chmod +x /app/docker-entrypoint.sh

EXPOSE 5100

HEALTHCHECK --interval=30s --timeout=10s --start-period=300s --retries=3 \
    CMD curl -fsS http://127.0.0.1:5100/health || exit 1

CMD ["/app/docker-entrypoint.sh"]
