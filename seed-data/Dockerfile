# One-shot seed container.
#
# Deliberately a separate image from either service. Neither service should own
# the other's seed data, and in PA-26 this same container gains a second target
# database.
FROM python:3.13-slim

WORKDIR /seed

# Dependencies first, so the layer caches across changes to the scripts and the
# working set.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# load.py reads working-set/ only. raw/ is excluded by .dockerignore: it is
# around 79 MB, it is gitignored, and pass 1 runs on the host rather than here.
COPY load.py filter_synthea.py ./
COPY working-set/ ./working-set/

# Runs as nobody. The seed job needs no write access to its own filesystem.
USER 65534:65534

CMD ["python", "load.py"]
