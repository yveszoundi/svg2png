import java.awt.RenderingHints;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.awt.Color;

import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.codec.binary.Base64;

public class SvgToPng {

    private Color hex2Rgb(String colorStr) {
        if (colorStr == null || colorStr.trim().isEmpty())
            throw new IllegalArgumentException("The background color must be specified!!");

        if (!colorStr.trim().startsWith("#")) {
            try {
                return (Color) Color.class.getDeclaredField(colorStr.trim().toUpperCase()).get(null);
            } catch (Exception e) {
                throw new RuntimeException("Could not parse color with name: '%s'. Maybe you want to provide the hex string value instead?? i.e. #ffffff");
            }
        }

        return new Color(
                         Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                         Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                         Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }

    private void toPngFromReader(Reader r, OutputStream resultByteStream, String backgroundColor) throws Exception {
        TranscoderInput transcoderInput = new TranscoderInput(r);
        TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

        PNGTranscoder pngTranscoder = new PNGTranscoder() {
                @Override
                protected ImageRenderer createRenderer() {
                    ImageRenderer r = super.createRenderer();

                    RenderingHints rh = r.getRenderingHints();

                    rh.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
                    rh.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC));
                    rh.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
                    rh.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY));
                    rh.add(new RenderingHints(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE));
                    rh.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
                    rh.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE));
                    rh.add(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON));
                    rh.add(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

                    r.setRenderingHints(rh);

                    return r;
                }
            };

        if (backgroundColor != null && backgroundColor.trim().length() != 0)
            pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, hex2Rgb(backgroundColor.trim()));

        pngTranscoder.transcode(transcoderInput, transcoderOutput);
    }

    public void toPngFileFromString(String svg, String backgroundColor, String outputFile) throws Exception {
        try (FileOutputStream resultByteStream = new FileOutputStream(new File(outputFile))) {
            toPngFromReader(new StringReader(svg), resultByteStream, backgroundColor);
        }
    }

    public String toPngBase64FromString(String svg, String backgroundColor) throws Exception {
        byte[] imageBytes = toPngBytesFromString(svg, backgroundColor);

        return Base64.encodeBase64String(imageBytes);
    }

    public byte[] toPngBytesFromString(String svg, String backgroundColor) throws Exception {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            toPngFromReader(new StringReader(svg), bos, backgroundColor);

            return bos.toByteArray();
        }
    }

}
