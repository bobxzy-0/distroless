# Release Process

Images in this repository are built and released in the `gcr.io/distroless` repository on the [Google Container Registry](https://cloud.google.com/container-registry/).

## How images are published

There are two supported publishing paths:

| Path | When to use |
|------|-------------|
| **Google Cloud Build** (original) | Triggered automatically on every commit to `main` via [cloudbuild.yaml](.cloudbuild/cloudbuild.yaml). Used by the upstream `gcr.io/distroless` project. |
| **GitHub Actions** (this repo) | Triggered automatically on every push to `main` via [.github/workflows/publish.yaml](.github/workflows/publish.yaml). Suitable for forks that publish to their own GCR project. |

Both paths ultimately run `bazel run :sign_and_push --config=release` which builds every OCI image, pushes it to GCR with `crane`, and signs it keylessly with `cosign`.

---

## GitHub Actions publishing — setup guide

### 1. Create a GCP Workload Identity Pool for GitHub Actions

```sh
PROJECT_ID="your-gcp-project-id"
POOL_NAME="github-actions-pool"
PROVIDER_NAME="github-provider"
REPO="your-org/your-repo"   # e.g. bobxzy-0/distroless

# Create the pool
gcloud iam workload-identity-pools create "$POOL_NAME" \
  --project="$PROJECT_ID" \
  --location="global" \
  --display-name="GitHub Actions Pool"

# Create the provider (maps GitHub OIDC tokens to GCP identities)
gcloud iam workload-identity-pools providers create-oidc "$PROVIDER_NAME" \
  --project="$PROJECT_ID" \
  --location="global" \
  --workload-identity-pool="$POOL_NAME" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="attribute.repository == '${REPO}'"
```

### 2. Create a service account and grant it registry write access

```sh
SA_NAME="ci-publisher"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts create "$SA_NAME" \
  --project="$PROJECT_ID" \
  --display-name="Distroless CI Publisher"

# Grant write access to GCR (backed by Cloud Storage)
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.admin"

# Allow the Workload Identity Pool to impersonate this service account
POOL_ID=$(gcloud iam workload-identity-pools describe "$POOL_NAME" \
  --project="$PROJECT_ID" --location="global" \
  --format="value(name)")

gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --project="$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/${POOL_ID}/attribute.repository/${REPO}"
```

### 3. Add the required secrets to your GitHub repository

Navigate to **Settings → Secrets and variables → Actions → New repository secret** and add:

| Secret name | Value |
|---|---|
| `GCP_PROJECT_ID` | Your GCP project ID (e.g. `my-project-123456`) |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Full resource name of the provider — output of `gcloud iam workload-identity-pools providers describe "$PROVIDER_NAME" --project="$PROJECT_ID" --location="global" --workload-identity-pool="$POOL_NAME" --format="value(name)"` |
| `GCP_SERVICE_ACCOUNT` | Service account email (e.g. `ci-publisher@my-project-123456.iam.gserviceaccount.com`) |
| `COSIGN_KEYLESS_EMAIL` | Same service account email — passed to `cosign sign` as the keyless identity |

### 4. Verify the workflow

Push a commit to `main` (or use **Actions → Publish Images → Run workflow**).  
The workflow will:

1. Authenticate to GCP using the Workload Identity Federation token.
2. Configure Docker credentials for `gcr.io`.
3. Build all distroless images with Bazel.
4. Push each image to `gcr.io/<GCP_PROJECT_ID>/<image>:<tag>`.
5. Sign each image keylessly with `cosign`.

---

## Google Cloud Build publishing (original)

Images are automatically built and pushed every commit, according to the policy defined in [cloudbuild.yaml](.cloudbuild/cloudbuild.yaml). The build script is [.cloudbuild/release.sh](.cloudbuild/release.sh).
