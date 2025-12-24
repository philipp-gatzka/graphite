#!/usr/bin/env bash
#
# Setup script for new collaborators
# Configures git hooks, submodules, and other project settings
#

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "Setting up graphite development environment..."
echo ""

# Initialize and update submodules
echo "Initializing submodules..."
git submodule update --init --recursive
echo "  ✓ Submodules initialized (wiki/)"

# Configure git to use project hooks
echo "Configuring git hooks..."
git config core.hooksPath .githooks
echo "  ✓ Git hooks configured (.githooks/)"

echo ""
echo "Setup complete!"
echo ""
echo "Repository structure:"
echo "  wiki/       - Documentation (submodule)"
echo "  .githooks/  - Shared git hooks"
echo ""
echo "Active hooks:"
echo "  - commit-msg: Enforces format #<issue-id> <message-in-lowercase>"
echo ""
