package org.zywx.wbpalmstar.plugin.uexzxing.client.result;


import java.util.Hashtable;
import java.util.Vector;

import org.zywx.wbpalmstar.plugin.uexzxing.Result;


final class SMSMMSResultParser extends ResultParser {

  private SMSMMSResultParser() {
  }

  public static SMSParsedResult parse(Result result) {
    String rawText = result.getText();
    if (rawText == null) {
      return null;
    }
    if (!(rawText.startsWith("sms:") || rawText.startsWith("SMS:") ||
          rawText.startsWith("mms:") || rawText.startsWith("MMS:"))) {
      return null;
    }

    // Check up front if this is a URI syntax string with query arguments
    Hashtable<?, ?> nameValuePairs = parseNameValuePairs(rawText);
    String subject = null;
    String body = null;
    boolean querySyntax = false;
    if (nameValuePairs != null && !nameValuePairs.isEmpty()) {
      subject = (String) nameValuePairs.get("subject");
      body = (String) nameValuePairs.get("body");
      querySyntax = true;
    }

    // Drop sms, query portion
    int queryStart = rawText.indexOf('?', 4);
    String smsURIWithoutQuery;
    // If it's not query syntax, the question mark is part of the subject or message
    if (queryStart < 0 || !querySyntax) {
      smsURIWithoutQuery = rawText.substring(4);
    } else {
      smsURIWithoutQuery = rawText.substring(4, queryStart);
    }

    int lastComma = -1;
    int comma;
    Vector<String> numbers = new Vector<String>(1);
    Vector<String> vias = new Vector<String>(1);
    while ((comma = smsURIWithoutQuery.indexOf(',', lastComma + 1)) > lastComma) {
      String numberPart = smsURIWithoutQuery.substring(lastComma + 1, comma);
      addNumberVia(numbers, vias, numberPart);
      lastComma = comma;
    }
    addNumberVia(numbers, vias, smsURIWithoutQuery.substring(lastComma + 1));    

    return new SMSParsedResult(toStringArray(numbers), toStringArray(vias), subject, body);
  }

  private static void addNumberVia(Vector<String> numbers, Vector<String> vias, String numberPart) {
    int numberEnd = numberPart.indexOf(';');
    if (numberEnd < 0) {
      numbers.addElement(numberPart);
      vias.addElement(null);
    } else {
      numbers.addElement(numberPart.substring(0, numberEnd));
      String maybeVia = numberPart.substring(numberEnd + 1);
      String via;
      if (maybeVia.startsWith("via=")) {
        via = maybeVia.substring(4);
      } else {
        via = null;
      }
      vias.addElement(via);
    }
  }

}