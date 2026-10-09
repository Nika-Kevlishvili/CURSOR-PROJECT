#!/bin/bash

# Script to find changed files in phoenix-core-lib by matching version and author
# Usage: ./find-gitlab-changes.sh <version> <commit_author> <gitlab_token> <project_id> <gitlab_url> <base_url>
#
# Returns: Comma-separated list of changed files or empty string if not found

VERSION_INPUT="$1"
AUTHOR="$2"
GITLAB_TOKEN="$3"
PHOENIX_CORE_LIB_PROJECT_ID="$4"
GITLAB_URL="$5"
BASE_URL="$6"

# Extract just the version number if full line was passed
# Handles both "1.26.26-SNAPSHOT" and "def devVersion = '1.26.26-SNAPSHOT'"
if [[ "$VERSION_INPUT" =~ def.*devVersion.*=.*\'([^\']+)\' ]]; then
  VERSION="${BASH_REMATCH[1]}"
else
  VERSION="$VERSION_INPUT"
fi

# Determine branch based on BASE_URL
if [[ "$BASE_URL" == *"10.236.20.11:8091"* ]]; then
  BRANCH="dev"
elif [[ "$BASE_URL" == *"10.236.20.81:8091"* ]]; then
  BRANCH="dev-fix"
else
  BRANCH="dev"  # Default to dev if unknown
fi

echo "Searching phoenix-core-lib pipelines for changes" >&2
echo "Version: $VERSION" >&2
echo "Author: $AUTHOR" >&2
echo "Branch: $BRANCH (based on BASE_URL: $BASE_URL)" >&2

  # Check if this is a GitHub no-reply email or real GitLab email
  if [[ "$AUTHOR" =~ @users\.noreply\.github\.com$ ]]; then
    echo "GitHub no-reply email detected - cannot lookup GitLab username" >&2
    echo "Searching pipelines by commit author instead..." >&2
    GITLAB_USERNAME=""
  else
    # Try to get user info from GitLab API using email search
    echo "Looking up GitLab username for: $AUTHOR" >&2
    
    SEARCH_TERM=$(echo "$AUTHOR" | cut -d'@' -f1)
    echo "Search term: $SEARCH_TERM" >&2  USER_SEARCH=$(curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
    "$GITLAB_URL/api/v4/users?search=$SEARCH_TERM" || echo "")
  
  if [ -n "$USER_SEARCH" ]; then
    GITLAB_USERNAME=$(echo "$USER_SEARCH" | grep -o '"username":"[^"]*"' | head -n1 | sed 's/"username":"\([^"]*\)"/\1/' || echo "")
  else
    GITLAB_USERNAME=""
  fi
fi

if [ -z "$GITLAB_USERNAME" ]; then
  echo "Searching pipelines without username filter (will verify author per commit)..." >&2
  PIPELINES_JSON=$(curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
    "$GITLAB_URL/api/v4/projects/$PHOENIX_CORE_LIB_PROJECT_ID/pipelines?status=success&ref=$BRANCH&per_page=50")
else
  echo "Found GitLab username: $GITLAB_USERNAME" >&2
  echo "Fetching pipelines for user $GITLAB_USERNAME on $BRANCH branch..." >&2
  PIPELINES_JSON=$(curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
    "$GITLAB_URL/api/v4/projects/$PHOENIX_CORE_LIB_PROJECT_ID/pipelines?status=success&ref=$BRANCH&username=$GITLAB_USERNAME&per_page=20")
fi

# Extract pipeline SHAs using grep/sed
PIPELINE_SHAS=$(echo "$PIPELINES_JSON" | grep -o '"sha":"[^"]*"' | sed 's/"sha":"\([^"]*\)"/\1/')

if [ -z "$PIPELINE_SHAS" ]; then
  echo "No pipelines found in phoenix-core-lib" >&2
  echo ""
  exit 0
fi

PIPELINE_COUNT=$(echo "$PIPELINE_SHAS" | wc -l | tr -d ' ')
echo "Found $PIPELINE_COUNT pipelines to check" >&2

# Find pipeline where build.gradle version matches (author check removed - unreliable due to inconsistent emails)
MATCHING_COMMIT=""
for COMMIT_SHA in $PIPELINE_SHAS; do
  echo "Checking commit: $COMMIT_SHA" >&2
  
  # Check version in build.gradle (primary matching criteria)
  BUILD_GRADLE=$(curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
    "$GITLAB_URL/api/v4/projects/$PHOENIX_CORE_LIB_PROJECT_ID/repository/files/build.gradle/raw?ref=$COMMIT_SHA" \
    2>/dev/null || echo "")
  
  if echo "$BUILD_GRADLE" | grep -q "def devVersion = '$VERSION'"; then
    echo "  Version matches!" >&2
    
    # Optional: Log commit author for reference (not used for filtering)
    COMMIT_INFO=$(curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
      "$GITLAB_URL/api/v4/projects/$PHOENIX_CORE_LIB_PROJECT_ID/repository/commits/$COMMIT_SHA")
    COMMIT_AUTHOR_EMAIL=$(echo "$COMMIT_INFO" | grep -o '"author_email":"[^"]*"' | sed 's/"author_email":"\([^"]*\)"/\1/')
    echo "  Commit author: $COMMIT_AUTHOR_EMAIL" >&2
    
    MATCHING_COMMIT=$COMMIT_SHA
    break
  else
    echo "  Version mismatch" >&2
  fi
done

if [ -z "$MATCHING_COMMIT" ]; then
  echo "No matching version found in recent pipelines" >&2
  echo ""
  exit 0
fi

echo "Getting changed files from commit: $MATCHING_COMMIT" >&2

# Get changed files from the matching commit (no jq)
DIFF_JSON=$(curl -s --header "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  "$GITLAB_URL/api/v4/projects/$PHOENIX_CORE_LIB_PROJECT_ID/repository/commits/$MATCHING_COMMIT/diff")

# Extract new_path values using grep and sed
CHANGED_FILES=$(echo "$DIFF_JSON" | grep -o '"new_path":"[^"]*"' | sed 's/"new_path":"\([^"]*\)"/\1/' | paste -sd "," -)

echo "Changed files in phoenix-core-lib:" >&2
echo "$CHANGED_FILES" | tr ',' '\n' >&2

# Output the changed files (comma-separated) to stdout (this is what gets captured)
echo "$CHANGED_FILES"
