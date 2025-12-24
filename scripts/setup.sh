#!/usr/bin/env bash
#
# Setup script for new collaborators
# Configures git hooks and other project settings
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Setting up graphite development environment..."
echo ""

# Configure git to use project hooks
echo "Configuring git hooks..."
git config core.hooksPath .githooks
echo "  ✓ Git hooks configured (.githooks/)"

echo ""
echo "Setup complete!"
echo ""
echo "The following hooks are now active:"
echo "  - commit-msg: Enforces commit message format (#<issue-id> <message-in-lowercase>)"
echo ""
