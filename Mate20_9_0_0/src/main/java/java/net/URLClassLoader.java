package java.net;

import java.io.Closeable;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import sun.misc.Resource;
import sun.misc.URLClassPath;
import sun.net.www.ParseUtil;
import sun.net.www.protocol.file.FileURLConnection;
import sun.security.util.SecurityConstants;
import sun.util.locale.LanguageTag;

public class URLClassLoader extends SecureClassLoader implements Closeable {
    private final AccessControlContext acc;
    private WeakHashMap<Closeable, Void> closeables = new WeakHashMap();
    private final URLClassPath ucp;

    public URLClassLoader(URL[] urls, ClassLoader parent) {
        super(parent);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, this.acc);
    }

    URLClassLoader(URL[] urls, ClassLoader parent, AccessControlContext acc) {
        super(parent);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.acc = acc;
        this.ucp = new URLClassPath(urls, acc);
    }

    public URLClassLoader(URL[] urls) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, this.acc);
    }

    URLClassLoader(URL[] urls, AccessControlContext acc) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.acc = acc;
        this.ucp = new URLClassPath(urls, acc);
    }

    public URLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(parent);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkCreateClassLoader();
        }
        this.acc = AccessController.getContext();
        this.ucp = new URLClassPath(urls, factory, this.acc);
    }

    public InputStream getResourceAsStream(String name) {
        URL url = getResource(name);
        if (url == null) {
            return null;
        }
        try {
            URLConnection urlc = url.openConnection();
            InputStream is = urlc.getInputStream();
            if (urlc instanceof JarURLConnection) {
                JarFile jar = ((JarURLConnection) urlc).getJarFile();
                synchronized (this.closeables) {
                    if (!this.closeables.containsKey(jar)) {
                        this.closeables.put(jar, null);
                    }
                }
            } else if (urlc instanceof FileURLConnection) {
                synchronized (this.closeables) {
                    this.closeables.put(is, null);
                }
            }
            return is;
        } catch (IOException e) {
            return null;
        }
    }

    public void close() throws IOException {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(new RuntimePermission("closeClassLoader"));
        }
        List<IOException> errors = this.ucp.closeLoaders();
        synchronized (this.closeables) {
            for (Closeable c : this.closeables.keySet()) {
                try {
                    c.close();
                } catch (IOException ioex) {
                    errors.add(ioex);
                }
            }
            this.closeables.clear();
        }
        if (!errors.isEmpty()) {
            IOException firstex = (IOException) errors.remove(0);
            for (IOException error : errors) {
                firstex.addSuppressed(error);
            }
            throw firstex;
        }
    }

    protected void addURL(URL url) {
        this.ucp.addURL(url);
    }

    public URL[] getURLs() {
        return this.ucp.getURLs();
    }

    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        try {
            Class<?> result = (Class) AccessController.doPrivileged(new PrivilegedExceptionAction<Class<?>>() {
                public Class<?> run() throws ClassNotFoundException {
                    Resource res = URLClassLoader.this.ucp.getResource(name.replace('.', '/').concat(".class"), false);
                    if (res == null) {
                        return null;
                    }
                    try {
                        return URLClassLoader.this.defineClass(name, res);
                    } catch (IOException e) {
                        throw new ClassNotFoundException(name, e);
                    }
                }
            }, this.acc);
            if (result != null) {
                return result;
            }
            throw new ClassNotFoundException(name);
        } catch (PrivilegedActionException pae) {
            throw ((ClassNotFoundException) pae.getException());
        }
    }

    private Package getAndVerifyPackage(String pkgname, Manifest man, URL url) {
        Package pkg = getPackage(pkgname);
        if (pkg != null) {
            StringBuilder stringBuilder;
            if (pkg.isSealed()) {
                if (!pkg.isSealed(url)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sealing violation: package ");
                    stringBuilder.append(pkgname);
                    stringBuilder.append(" is sealed");
                    throw new SecurityException(stringBuilder.toString());
                }
            } else if (man != null && isSealed(pkgname, man)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("sealing violation: can't seal package ");
                stringBuilder.append(pkgname);
                stringBuilder.append(": already loaded");
                throw new SecurityException(stringBuilder.toString());
            }
        }
        return pkg;
    }

    private void definePackageInternal(String pkgname, Manifest man, URL url) {
        if (getAndVerifyPackage(pkgname, man, url) != null) {
            return;
        }
        if (man != null) {
            try {
                definePackage(pkgname, man, url);
                return;
            } catch (IllegalArgumentException e) {
                if (getAndVerifyPackage(pkgname, man, url) == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Cannot find package ");
                    stringBuilder.append(pkgname);
                    throw new AssertionError(stringBuilder.toString());
                }
                return;
            }
        }
        definePackage(pkgname, null, null, null, null, null, null, null);
    }

    private Class<?> defineClass(String name, Resource res) throws IOException {
        String str = name;
        long t0 = System.nanoTime();
        int i = str.lastIndexOf(46);
        URL url = res.getCodeSourceURL();
        if (i != -1) {
            definePackageInternal(str.substring(null, i), res.getManifest(), url);
        }
        ByteBuffer bb = res.getByteBuffer();
        if (bb != null) {
            return defineClass(str, bb, new CodeSource(url, res.getCodeSigners()));
        }
        byte[] b = res.getBytes();
        return defineClass(str, b, 0, b.length, new CodeSource(url, res.getCodeSigners()));
    }

    protected Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        String str = name;
        String sealed = null;
        String specVersion = null;
        String specVendor = null;
        String implTitle = null;
        String implVersion = null;
        String implVendor = null;
        String sealed2 = null;
        Attributes attr = man.getAttributes(str.replace('.', '/').concat("/"));
        if (attr != null) {
            sealed = attr.getValue(Name.SPECIFICATION_TITLE);
            specVersion = attr.getValue(Name.SPECIFICATION_VERSION);
            specVendor = attr.getValue(Name.SPECIFICATION_VENDOR);
            implTitle = attr.getValue(Name.IMPLEMENTATION_TITLE);
            implVersion = attr.getValue(Name.IMPLEMENTATION_VERSION);
            implVendor = attr.getValue(Name.IMPLEMENTATION_VENDOR);
            sealed2 = attr.getValue(Name.SEALED);
        }
        Attributes attr2 = man.getMainAttributes();
        if (attr2 != null) {
            if (sealed == null) {
                sealed = attr2.getValue(Name.SPECIFICATION_TITLE);
            }
            if (specVersion == null) {
                specVersion = attr2.getValue(Name.SPECIFICATION_VERSION);
            }
            if (specVendor == null) {
                specVendor = attr2.getValue(Name.SPECIFICATION_VENDOR);
            }
            if (implTitle == null) {
                implTitle = attr2.getValue(Name.IMPLEMENTATION_TITLE);
            }
            if (implVersion == null) {
                implVersion = attr2.getValue(Name.IMPLEMENTATION_VERSION);
            }
            if (implVendor == null) {
                implVendor = attr2.getValue(Name.IMPLEMENTATION_VENDOR);
            }
            if (sealed2 == null) {
                sealed2 = attr2.getValue(Name.SEALED);
            }
        }
        return definePackage(str, sealed, specVersion, specVendor, implTitle, implVersion, implVendor, "true".equalsIgnoreCase(sealed2) ? url : null);
    }

    private boolean isSealed(String name, Manifest man) {
        Attributes attr = man.getAttributes(name.replace('.', '/').concat("/"));
        String sealed = null;
        if (attr != null) {
            sealed = attr.getValue(Name.SEALED);
        }
        if (sealed == null) {
            Attributes mainAttributes = man.getMainAttributes();
            attr = mainAttributes;
            if (mainAttributes != null) {
                sealed = attr.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    public URL findResource(final String name) {
        URL url = (URL) AccessController.doPrivileged(new PrivilegedAction<URL>() {
            public URL run() {
                return URLClassLoader.this.ucp.findResource(name, true);
            }
        }, this.acc);
        return url != null ? this.ucp.checkURL(url) : null;
    }

    public Enumeration<URL> findResources(String name) throws IOException {
        final Enumeration<URL> e = this.ucp.findResources(name, true);
        return new Enumeration<URL>() {
            private URL url = null;

            private boolean next() {
                boolean z = true;
                if (this.url != null) {
                    return true;
                }
                do {
                    URL u = (URL) AccessController.doPrivileged(new PrivilegedAction<URL>() {
                        public URL run() {
                            if (e.hasMoreElements()) {
                                return (URL) e.nextElement();
                            }
                            return null;
                        }
                    }, URLClassLoader.this.acc);
                    if (u == null) {
                        break;
                    }
                    this.url = URLClassLoader.this.ucp.checkURL(u);
                } while (this.url == null);
                if (this.url == null) {
                    z = false;
                }
                return z;
            }

            public URL nextElement() {
                if (next()) {
                    URL u = this.url;
                    this.url = null;
                    return u;
                }
                throw new NoSuchElementException();
            }

            public boolean hasMoreElements() {
                return next();
            }
        };
    }

    protected PermissionCollection getPermissions(CodeSource codesource) {
        URLConnection urlConnection;
        Permission p;
        PermissionCollection perms = super.getPermissions(codesource);
        URL url = codesource.getLocation();
        try {
            urlConnection = url.openConnection();
            p = urlConnection.getPermission();
        } catch (IOException e) {
            p = null;
            urlConnection = null;
        }
        String path;
        StringBuilder stringBuilder;
        if (p instanceof FilePermission) {
            path = p.getName();
            if (path.endsWith(File.separator)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(path);
                stringBuilder.append(LanguageTag.SEP);
                p = new FilePermission(stringBuilder.toString(), "read");
            }
        } else if (p == null && url.getProtocol().equals("file")) {
            path = ParseUtil.decode(url.getFile().replace('/', File.separatorChar));
            if (path.endsWith(File.separator)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(path);
                stringBuilder.append(LanguageTag.SEP);
                path = stringBuilder.toString();
            }
            p = new FilePermission(path, "read");
        } else {
            URL locUrl = url;
            if (urlConnection instanceof JarURLConnection) {
                locUrl = ((JarURLConnection) urlConnection).getJarFileURL();
            }
            String host = locUrl.getHost();
            if (host != null && host.length() > 0) {
                p = new SocketPermission(host, SecurityConstants.SOCKET_CONNECT_ACCEPT_ACTION);
            }
        }
        if (p != null) {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                final Permission fp = p;
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    public Void run() throws SecurityException {
                        sm.checkPermission(fp);
                        return null;
                    }
                }, this.acc);
            }
            perms.add(p);
        }
        return perms;
    }

    public static URLClassLoader newInstance(final URL[] urls, final ClassLoader parent) {
        final AccessControlContext acc = AccessController.getContext();
        return (URLClassLoader) AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            public URLClassLoader run() {
                return new FactoryURLClassLoader(urls, parent, acc);
            }
        });
    }

    public static URLClassLoader newInstance(final URL[] urls) {
        final AccessControlContext acc = AccessController.getContext();
        return (URLClassLoader) AccessController.doPrivileged(new PrivilegedAction<URLClassLoader>() {
            public URLClassLoader run() {
                return new FactoryURLClassLoader(urls, acc);
            }
        });
    }

    static {
        ClassLoader.registerAsParallelCapable();
    }
}
