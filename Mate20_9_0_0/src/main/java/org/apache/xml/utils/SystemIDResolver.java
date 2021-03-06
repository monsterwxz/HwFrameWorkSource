package org.apache.xml.utils;

import java.io.File;
import javax.xml.transform.TransformerException;
import org.apache.xml.utils.URI.MalformedURIException;
import org.apache.xpath.compiler.PsuedoNames;

public class SystemIDResolver {
    public static String getAbsoluteURIFromRelative(String localPath) {
        if (localPath == null || localPath.length() == 0) {
            return "";
        }
        String urlString;
        String absolutePath = localPath;
        if (!isAbsolutePath(localPath)) {
            try {
                absolutePath = getAbsolutePathFromRelativePath(localPath);
            } catch (SecurityException e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("file:");
                stringBuilder.append(localPath);
                return stringBuilder.toString();
            }
        }
        StringBuilder stringBuilder2;
        if (absolutePath == null) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("file:");
            stringBuilder2.append(localPath);
            urlString = stringBuilder2.toString();
        } else if (absolutePath.startsWith(File.separator)) {
            urlString = new StringBuilder();
            urlString.append("file://");
            urlString.append(absolutePath);
            urlString = urlString.toString();
        } else {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("file:///");
            stringBuilder2.append(absolutePath);
            urlString = stringBuilder2.toString();
        }
        return replaceChars(urlString);
    }

    private static String getAbsolutePathFromRelativePath(String relativePath) {
        return new File(relativePath).getAbsolutePath();
    }

    public static boolean isAbsoluteURI(String systemId) {
        boolean z = false;
        if (isWindowsAbsolutePath(systemId)) {
            return false;
        }
        int fragmentIndex = systemId.indexOf(35);
        int queryIndex = systemId.indexOf(63);
        int slashIndex = systemId.indexOf(47);
        int colonIndex = systemId.indexOf(58);
        int index = systemId.length() - 1;
        if (fragmentIndex > 0) {
            index = fragmentIndex;
        }
        if (queryIndex > 0 && queryIndex < index) {
            index = queryIndex;
        }
        if (slashIndex > 0 && slashIndex < index) {
            index = slashIndex;
        }
        if (colonIndex > 0 && colonIndex < index) {
            z = true;
        }
        return z;
    }

    public static boolean isAbsolutePath(String systemId) {
        if (systemId == null) {
            return false;
        }
        return new File(systemId).isAbsolute();
    }

    private static boolean isWindowsAbsolutePath(String systemId) {
        if (isAbsolutePath(systemId) && systemId.length() > 2 && systemId.charAt(1) == ':' && Character.isLetter(systemId.charAt(0)) && (systemId.charAt(2) == '\\' || systemId.charAt(2) == '/')) {
            return true;
        }
        return false;
    }

    private static String replaceChars(String str) {
        StringBuffer buf = new StringBuffer(str);
        int length = buf.length();
        int i = 0;
        while (i < length) {
            char currentChar = buf.charAt(i);
            if (currentChar == ' ') {
                buf.setCharAt(i, '%');
                buf.insert(i + 1, "20");
                length += 2;
                i += 2;
            } else if (currentChar == '\\') {
                buf.setCharAt(i, '/');
            }
            i++;
        }
        return buf.toString();
    }

    public static String getAbsoluteURI(String systemId) {
        String absoluteURI = systemId;
        if (!isAbsoluteURI(systemId)) {
            return getAbsoluteURIFromRelative(systemId);
        }
        if (!systemId.startsWith("file:")) {
            return systemId;
        }
        String str = systemId.substring(5);
        if (str == null || !str.startsWith(PsuedoNames.PSEUDONAME_ROOT)) {
            return getAbsoluteURIFromRelative(systemId.substring(5));
        }
        if (str.startsWith("///") || !str.startsWith("//")) {
            int secondColonIndex = systemId.indexOf(58, 5);
            if (secondColonIndex > 0) {
                String localPath = systemId.substring(secondColonIndex - 1);
                try {
                    if (!isAbsolutePath(localPath)) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(systemId.substring(0, secondColonIndex - 1));
                        stringBuilder.append(getAbsolutePathFromRelativePath(localPath));
                        absoluteURI = stringBuilder.toString();
                    }
                } catch (SecurityException e) {
                    return systemId;
                }
            }
        }
        return replaceChars(absoluteURI);
    }

    public static String getAbsoluteURI(String urlString, String base) throws TransformerException {
        if (base == null) {
            return getAbsoluteURI(urlString);
        }
        try {
            return replaceChars(new URI(new URI(getAbsoluteURI(base)), urlString).toString());
        } catch (MalformedURIException mue) {
            throw new TransformerException(mue);
        }
    }
}
