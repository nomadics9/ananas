
# Ananas
Personal fork
## Screenshots
| Home                                | Library                             | Movie                           | Season                            | Episode                             |
|-------------------------------------|-------------------------------------|---------------------------------|-----------------------------------|-------------------------------------|
| ![Home](images/home-light-dark.png) | ![Library](images/library-dark.png) | ![Movie](images/movie-dark.png) | ![Season](images/season-dark.png) | ![Episode](images/episode-dark.png) |

## Features
- Completely native interface
- Supported media items: movies, series, seasons, episodes 
  - Direct play only, (no transcoding)
- Offline playback / downloads
- ExoPlayer
  - Video codecs: H.263, H.264, H.265, VP8, VP9, AV1 
    - Support depends on Android device
  - Audio codecs: Vorbis, Opus, FLAC, ALAC, PCM, MP3, AAC, AC-3, E-AC-3, DTS, DTS-HD, TrueHD 
    - Support provided by ExoPlayer FFmpeg extension
  - Subtitle codecs: SRT, VTT, SSA/ASS, PGSSUB
    - SSA/ASS has limited styling support see [this issue](https://github.com/google/ExoPlayer/issues/8435)
- mpv
  - Container formats: mkv, mov, mp4, avi
  - Video codecs: H.264, H.265, VP8, VP9, AV1
  - Audio codecs: Opus, FLAC, MP3, AAC, AC-3, E-AC-3, TrueHD, DTS, DTS-HD
  - Subtitle codecs: SRT, VTT, SSA/ASS, DVDSUB
  - Optionally force software decoding when hardware decoding has issues.
- Picture-in-picture mode
- Media chapters
  - Timeline markers
  - Chapter navigation gestures

## Planned features
- Android TV
- Websocket connection (Syncplay)
- Chromecast support


## License
This project is licensed under [GPLv3](LICENSE).


