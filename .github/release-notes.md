## What's new in AniTenshi 1.0.18

- **Very tall pages in long strip**: chapters stitched into a few PNG images taller than 32,767 pixels showed "The image couldn't be loaded", while the same image opened fine in the WebView. They now load: when the built-in decoder refuses an image, the reader falls back to Android's own. Such a page can take a few seconds to appear.
- **The reader says why a page didn't load**: under "The image couldn't be loaded" you now see the reason (for example the site's answer, such as HTTP 403, or the decoder's error) and, when the image did arrive but couldn't be shown, what it is: format, size in pixels and weight. Before, every failure showed the same sentence.
