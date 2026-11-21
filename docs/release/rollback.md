# Release rollback

Maven Central versions are immutable. Recovery depends on whether the
deployment was actually published.

## Before Central publishes

Use this when the deployment is validated but not released, or when upload or
validation failed.

1. Discard the Portal deployment if one exists.
2. Fix the source on the release branch.
3. Delete the local tag if it was never pushed:

   ```bash
   git tag -d "v$VERSION"
   ```

4. Re-run the local release preparation, signed build, and verification.
5. Upload a new `USER_MANAGED` deployment and validate it again.

If the bad tag or release commit was already pushed, do not force-rewrite
published history. Delete the remote tag only if the version was never released
on Central, then create a corrected release commit and tag.

## After Central publishes

Never re-upload, re-sign, or move the tag. Publish the next version:

```bash
# For a hotfix, fix on master first.
git switch master
# Apply and test the fix.
git commit -S -m 'Fix <issue>'
export FIX_COMMIT="$(git rev-parse HEAD)"

git switch release/1.0
git cherry-pick -S "$FIX_COMMIT"
# Add the changelog heading and run the normal patch release procedure.
```

Patch releases use `1.0.1`, `1.0.2`, and so on. A breaking change requires a
new minor or major line, depending on the compatibility policy.

## Rollback table

| State | Action |
|---|---|
| Local signed build failed | Fix, delete local tag if present, rebuild |
| Portal validation failed | Discard deployment, fix, re-upload |
| Deployment validated, not released | Discard or leave until ready; never edit the version |
| Published on Central | Never re-upload; publish the next patch/minor |
| Bad tag, not pushed, not published | Delete local tag; recreate from corrected commit |
| Bad tag, pushed, not published | Delete remote tag with explicit approval; recreate |
| Bad tag, published on Central | Keep tag; publish a new version |
| Release branch merged into master by mistake | Revert the merge; keep development on master |

## Related

[Release runbook](runbook.md) · [Publishing](publishing.md) ·
[Signing](signing.md)
