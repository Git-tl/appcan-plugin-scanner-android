package org.zywx.wbpalmstar.plugin.uexzxing.multi;


import java.util.Hashtable;
import java.util.Vector;

import org.zywx.wbpalmstar.plugin.uexzxing.BinaryBitmap;
import org.zywx.wbpalmstar.plugin.uexzxing.NotFoundException;
import org.zywx.wbpalmstar.plugin.uexzxing.Reader;
import org.zywx.wbpalmstar.plugin.uexzxing.ReaderException;
import org.zywx.wbpalmstar.plugin.uexzxing.Result;
import org.zywx.wbpalmstar.plugin.uexzxing.ResultPoint;

public final class GenericMultipleBarcodeReader implements MultipleBarcodeReader {

  private static final int MIN_DIMENSION_TO_RECUR = 100;

  private final Reader delegate;

  public GenericMultipleBarcodeReader(Reader delegate) {
    this.delegate = delegate;
  }

  public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
    return decodeMultiple(image, null);
  }

  @SuppressWarnings("rawtypes")
public Result[] decodeMultiple(BinaryBitmap image, Hashtable hints)
      throws NotFoundException {
    Vector results = new Vector();
    doDecodeMultiple(image, hints, results, 0, 0);
    if (results.isEmpty()) {
      throw NotFoundException.getNotFoundInstance();
    }
    int numResults = results.size();
    Result[] resultArray = new Result[numResults];
    for (int i = 0; i < numResults; i++) {
      resultArray[i] = (Result) results.elementAt(i);
    }
    return resultArray;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
private void doDecodeMultiple(BinaryBitmap image,
                                Hashtable hints,
                                Vector results,
                                int xOffset,
                                int yOffset) {
    Result result;
    try {
      result = delegate.decode(image, hints);
    } catch (ReaderException re) {
      return;
    }
    boolean alreadyFound = false;
    for (int i = 0; i < results.size(); i++) {
      Result existingResult = (Result) results.elementAt(i);
      if (existingResult.getText().equals(result.getText())) {
        alreadyFound = true;
        break;
      }
    }
    if (alreadyFound) {
      return;
    }
    results.addElement(translateResultPoints(result, xOffset, yOffset));
    ResultPoint[] resultPoints = result.getResultPoints();
    if (resultPoints == null || resultPoints.length == 0) {
      return;
    }
    int width = image.getWidth();
    int height = image.getHeight();
    float minX = width;
    float minY = height;
    float maxX = 0.0f;
    float maxY = 0.0f;
    for (int i = 0; i < resultPoints.length; i++) {
      ResultPoint point = resultPoints[i];
      float x = point.getX();
      float y = point.getY();
      if (x < minX) {
        minX = x;
      }
      if (y < minY) {
        minY = y;
      }
      if (x > maxX) {
        maxX = x;
      }
      if (y > maxY) {
        maxY = y;
      }
    }

    // Decode left of barcode
    if (minX > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, (int) minX, height),
                       hints, results, xOffset, yOffset);
    }
    // Decode above barcode
    if (minY > MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, 0, width, (int) minY),
                       hints, results, xOffset, yOffset);
    }
    // Decode right of barcode
    if (maxX < width - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop((int) maxX, 0, width - (int) maxX, height),
                       hints, results, xOffset + (int) maxX, yOffset);
    }
    // Decode below barcode
    if (maxY < height - MIN_DIMENSION_TO_RECUR) {
      doDecodeMultiple(image.crop(0, (int) maxY, width, height - (int) maxY),
                       hints, results, xOffset, yOffset + (int) maxY);
    }
  }

  private static Result translateResultPoints(Result result, int xOffset, int yOffset) {
    ResultPoint[] oldResultPoints = result.getResultPoints();
    ResultPoint[] newResultPoints = new ResultPoint[oldResultPoints.length];
    for (int i = 0; i < oldResultPoints.length; i++) {
      ResultPoint oldPoint = oldResultPoints[i];
      newResultPoints[i] = new ResultPoint(oldPoint.getX() + xOffset, oldPoint.getY() + yOffset);
    }
    return new Result(result.getText(), result.getRawBytes(), newResultPoints,
        result.getBarcodeFormat());
  }

}
