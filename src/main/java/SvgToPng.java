import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.gvt.renderer.ImageRenderer;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

public class SvgToPng {

	private Dimension getScaledDimension(Dimension imageSize, Dimension boundary) {
		double widthRatio = boundary.getWidth() / imageSize.getWidth();
		double heightRatio = boundary.getHeight() / imageSize.getHeight();
		double ratio = Math.min(widthRatio, heightRatio);

		return new Dimension((int) ((double) imageSize.width * ratio), (int) ((double) imageSize.height * ratio));
	}

	private Color hex2Rgb(String colorStr) {
		if (colorStr == null || colorStr.trim().isEmpty())
			throw new IllegalArgumentException("The background color must be specified!!");

		if (!colorStr.trim().startsWith("#")) {
			try {
				return (Color) Color.class.getDeclaredField(colorStr.trim().toUpperCase()).get(null);
			} catch (Exception e) {
				throw new RuntimeException(
						"Could not parse color with name: '%s'. Maybe you want to provide the hex string value instead?? i.e. #ffffff");
			}
		}

		return new Color(Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16),
				Integer.valueOf(colorStr.substring(5, 7), 16));
	}

	private Dimension svgSize(String svgText) throws Exception {
		String parser = XMLResourceDescriptor.getXMLParserClassName();
		SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
		Document doc = f.createDocument("", new CharArrayReader(svgText.toCharArray()));
		NamedNodeMap svgAttributes = doc.getFirstChild().getAttributes();
		String widthText = (svgAttributes.getNamedItem("width").getTextContent());
		String heightText = (svgAttributes.getNamedItem("height").getTextContent());

		int w = Integer.parseInt(widthText);
		int h = Integer.parseInt(heightText);

		return new Dimension(w, h);
	}

	private void toPngFromReader(Reader r, OutputStream resultByteStream, String backgroundColor, Dimension geometry)
			throws Exception {
		TranscoderInput transcoderInput = new TranscoderInput(r);
		TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);

		PNGTranscoder pngTranscoder = new PNGTranscoder() {
			@Override
			protected ImageRenderer createRenderer() {
				ImageRenderer r = super.createRenderer();

				RenderingHints rh = r.getRenderingHints();

				rh.add(new RenderingHints(RenderingHints.KEY_ALPHA_INTERPOLATION,
						RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY));
				rh.add(new RenderingHints(RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BICUBIC));
				rh.add(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON));
				rh.add(new RenderingHints(RenderingHints.KEY_COLOR_RENDERING,
						RenderingHints.VALUE_COLOR_RENDER_QUALITY));
				rh.add(new RenderingHints(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE));
				rh.add(new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY));
				rh.add(new RenderingHints(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE));
				rh.add(new RenderingHints(RenderingHints.KEY_FRACTIONALMETRICS,
						RenderingHints.VALUE_FRACTIONALMETRICS_ON));
				rh.add(new RenderingHints(RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON));

				r.setRenderingHints(rh);

				return r;
			}
		};

		if (backgroundColor != null && backgroundColor.trim().length() != 0)
			pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_BACKGROUND_COLOR, hex2Rgb(backgroundColor.trim()));

		if (geometry.getWidth() > 1D) {
			pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, Float.parseFloat("" + geometry.getWidth()));
			pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, Float.parseFloat("" + geometry.getHeight()));
		}

		pngTranscoder.transcode(transcoderInput, transcoderOutput);
	}

	public void toPngFileFromString(String svg, String backgroundColor, String outputFile, String geometry)
			throws Exception {
		try (FileOutputStream resultByteStream = new FileOutputStream(new File(outputFile))) {
			byte[] imageBytes = toPngBytesFromString(svg, backgroundColor, geometry);
			resultByteStream.write(imageBytes);
		}
	}

	public String toPngBase64FromString(String svg, String backgroundColor, String geometry) throws Exception {
		byte[] imageBytes = toPngBytesFromString(svg, backgroundColor, geometry);

		return Base64.encodeBase64String(imageBytes);
	}

	public byte[] toPngBytesFromString(String svg, String backgroundColor, String geometry) throws Exception {
		try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

			Dimension dim = new Dimension();

			if (geometry != null && geometry.trim().length() != 0) {
				String[] geomText = geometry.split("x");
				if (geomText.length != 2)
					throw new IllegalArgumentException(String.format(
							"Invalid geometry: '%s'. It must be in the format: '1024x768' (width x height)", geometry));

				try {
					int w = Integer.parseInt(geomText[0].trim());
					int h = Integer.parseInt(geomText[1].trim());
					Dimension oldImageSize = svgSize(svg);
					dim = getScaledDimension(oldImageSize, new Dimension(w, h));
				} catch (RuntimeException e) {
					throw new RuntimeException(
							"Please make sure that the geometry contains valid numbers (width x height). i.e. 1024x768",
							e);
				}
			}

			toPngFromReader(new StringReader(svg), bos, backgroundColor, dim);

			return bos.toByteArray();
		}
	}

}
