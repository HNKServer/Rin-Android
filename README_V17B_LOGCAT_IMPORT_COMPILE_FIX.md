# v17b logcat import compile fix

Base: v17.

Only change:

```rust
#[cfg(all(feature = "library", target_os = "android"))]
use crate::log_to_logcat;
```

is added to start.rs so the Android-only diagnostic markers compile.

No routing, response, asset hash, masterdata, WebUI, translation, or static-asset logic is changed from v17.
