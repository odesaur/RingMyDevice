# Releasing

This document describes how to publish a new release for RMD Server.

1. Build and test both the binary **and** the Docker container.
1. Update the `const VERSION = ...` in Go.
1. Update the version examples in the README.
1. Commit and push.
1. Tag the new release: `git tag v0.0.0` and push the tag: `git push --tags`.
1. Create a new release on GitHub: https://github.com/odesaur/RingMyDevice/releases.
1. Wait for the Docker image build to finish, and briefly test that the image works.
1. Use the `bundle.sh` script to create a ZIP with a pre-compiled binary. Upload it to the release artifacts.
