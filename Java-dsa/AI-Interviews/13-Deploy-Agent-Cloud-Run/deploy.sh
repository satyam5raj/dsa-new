#!/usr/bin/env bash
# Reference deployment script for AgentServer on Google Cloud Run.
# NOT run by run-all.sh. Read it, set the variables, and run the steps you need.
# Flags change over time: check `gcloud run deploy --help` for your gcloud version.
set -euo pipefail

PROJECT_ID="my-project"
REGION="us-central1"
SERVICE="acme-agent"
SA="agent-sa@${PROJECT_ID}.iam.gserviceaccount.com"

# 1) One-time setup: enable APIs
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com \
    secretmanager.googleapis.com aiplatform.googleapis.com --project "$PROJECT_ID"

# 2) A dedicated service account with least privilege
gcloud iam service-accounts create agent-sa --project "$PROJECT_ID" --display-name "Agent runtime"
gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:${SA}" --role roles/aiplatform.user        # call Vertex AI models
gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:${SA}" --role roles/datastore.user         # Firestore sessions

# 3) Secrets live in Secret Manager, never in the image or in plain env vars
printf '%s' "replace-with-a-real-token" | gcloud secrets create app-api-token --data-file=- --project "$PROJECT_ID"
gcloud secrets add-iam-policy-binding app-api-token --member "serviceAccount:${SA}" --role roles/secretmanager.secretAccessor --project "$PROJECT_ID"

# 4) Build and deploy from source (Cloud Build uses the Dockerfile in this folder)
gcloud run deploy "$SERVICE" \
    --source . \
    --project "$PROJECT_ID" --region "$REGION" \
    --service-account "$SA" \
    --no-allow-unauthenticated \
    --set-secrets "APP_API_TOKEN=app-api-token:latest" \
    --set-env-vars "MODEL_NAME=gemini-2.5-flash,GOOGLE_CLOUD_PROJECT=${PROJECT_ID}" \
    --cpu 1 --memory 1Gi \
    --concurrency 40 \
    --min-instances 1 --max-instances 20 \
    --timeout 300 \
    --cpu-boost

# 5) Call it (IAM-protected): the caller needs roles/run.invoker
URL=$(gcloud run services describe "$SERVICE" --region "$REGION" --project "$PROJECT_ID" --format 'value(status.url)')
curl -s -X POST "$URL/chat" \
    -H "Authorization: Bearer $(gcloud auth print-identity-token)" \
    -H "Content-Type: application/json" \
    -d '{"session_id":"s1","message":"Where is order A-1001?"}'
# Note: with Cloud Run IAM in front, the app's own APP_API_TOKEN check would need a separate header;
# in production pick ONE scheme (IAM identity tokens, or an API gateway in front).

# 6) Safer rollouts: deploy without traffic, then shift gradually
# gcloud run deploy "$SERVICE" --source . --region "$REGION" --no-traffic --tag canary
# gcloud run services update-traffic "$SERVICE" --region "$REGION" --to-tags canary=10
# gcloud run services update-traffic "$SERVICE" --region "$REGION" --to-latest

# Alternative for Google ADK agents (Python): the ADK CLI can package and deploy an agent to Cloud Run, e.g.
# adk deploy cloud_run --project "$PROJECT_ID" --region "$REGION" ./my_agent
