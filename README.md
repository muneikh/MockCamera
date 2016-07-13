# MockCamera
A mocking camera/video example with GLSurfaceView

Test Configuration
------------------
Inside `app/build.gradle` update the `buildConfigField "boolean", "AUTO_TEST", "false"` to `true` or `false`.
  * `true` will mock video from the `assets` folder to GLSurfaceView.
  * `false` will trigger camera stream from hardware.

```java
 if (BuildConfig.AUTO_TEST) {
    textureVideoInputSource = new VideoFileInputSource(this, "mock_input_video.mp4");
 } else {
    textureVideoInputSource = new CameraTextureVideoInputSource(this);
 }
```
