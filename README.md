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

License
-------

    Copyright 2016 Muneeb Sheikh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
