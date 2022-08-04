
module.exports = function (runtime, global) {

    var javax = Packages.javax;
    var MessageDigest = java.security.MessageDigest;
    var Base64 = android.util.Base64;
    var Crypto = com.stardust.autojs.core.cypto.Crypto;
    var Cipher = javax.crypto.Cipher;
    var SecretKeySpec = javax.crypto.spec.SecretKeySpec;
    var KeyPairGenerator = java.security.KeyPairGenerator;
    var X509EncodedKeySpec = java.security.spec.X509EncodedKeySpec;
    var PKCS8EncodedKeySpec = java.security.spec.PKCS8EncodedKeySpec;
    var KeyFactory = java.security.KeyFactory;
    var CipherOutputStream = javax.crypto.CipherOutputStream;
    var ByteArrayOutputStream = java.io.ByteArrayOutputStream;

    function $crypto() {

    }

    $crypto._input = function (input, options, callback) {
        if (options.input == 'file') {
            let fis = new java.io.FileInputStream(input);
            let buffer = util.java.array('byte', 4096);
            while (true) {
                let r = fis.read(buffer);
                if (r > 0) {
                    callback(buffer, 0, r);
                } else {
                    break;
                }
            }
            return;
        }
        if (options.input == 'base64') {
            input = Base64.decode(input, Base64.NO_WRAP);
        } else if (options.input == 'hex') {
            input = $crypto._fromHex(input);
        } else {
            let encoding = options.encoding || "utf-8";
            if (typeof (input) == 'string') {
                input = new java.lang.String(input).getBytes(encoding);
            }
        }
        callback(input, 0, input.length);
    }

    $crypto._output = function (bytes, options, defFormat) {
        let format = options.output || defFormat;
        if (format == 'base64') {
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        }
        if (format == 'bytes') {
            return bytes;
        }
        let encoding = options.encoding || "utf-8";
        if (format == 'string') {
            return String(new java.lang.String(bytes, encoding));
        }
        return $crypto._toHex(bytes);
    }

    $crypto._toHex = function (bytes) {
        return Crypto.toHex(bytes);
    }

    $crypto._fromHex = function (bytes) {
        return Crypto.fromHex(bytes);
    }

    $crypto._cipher = function (data, mode, key, algorithm, options) {
        options = options || {};
        let cipher = Cipher.getInstance(algorithm);
        cipher.init(mode, key.toKeySpec(algorithm));
        let os;
        let isFile = options.output == 'file' && options.dest;
        if (isFile) {
            os = new java.io.FileOutputStream(options.dest);
        } else {
            os = new ByteArrayOutputStream();
        }
        let cos = new CipherOutputStream(os, cipher);
        $crypto._input(data, options, (bytes, start, length) => {
            cos.write(bytes, start, length);
        });
        cos.close();
        os.close();
        if (!isFile) {
            let result = os.toByteArray();
            return $crypto._output(result, options, 'bytes');
        }
    }

    $crypto.decrypt = function (data, key, algorithm, options) {
        return $crypto._cipher(data, Cipher.DECRYPT_MODE, key, algorithm, options);
    }

    $crypto.digest = function (message, algorithm, options) {
        options = options || {};
        let instance = MessageDigest.getInstance(algorithm);
        $crypto._input(message, options, (bytes, start, length) => {
            instance.update(bytes, start, length);
        });
        let bytes = instance.digest();
        return $crypto._output(bytes, options, 'hex');
    }

    $crypto.encrypt = function (data, key, algorithm, options) {
        return $crypto._cipher(data, Cipher.ENCRYPT_MODE, key, algorithm, options);
    }

    $crypto.generateKeyPair = function (algorithm, length) {
        let generator = KeyPairGenerator.getInstance(algorithm);
        length = length || 256;
        generator.initialize(length);
        let keyPair = generator.generateKeyPair();
        return new KeyPair(keyPair.getPublic().getEncoded(), keyPair.getPrivate().getEncoded());
    }

    $crypto.Key = Key;

    $crypto.KeyPair = KeyPair;

    function Key(data, options) {
        options = options || {};
        this.keyPair = options.keyPair;
        let bos = new java.io.ByteArrayOutputStream();
        $crypto._input(data, options, (bytes, start, length) => {
            bos.write(bytes, start, length);
        });
        this.data = bos.toByteArray();
    }

    Key.prototype.toKeySpec = function (algorithm) {
        let i = algorithm.indexOf("/");
        if (i >= 0) {
            algorithm = algorithm.substring(0, i);
        }
        if (algorithm == 'RSA') {
            if (this.keyPair == 'public') {
                return KeyFactory.getInstance(algorithm).generatePublic(new X509EncodedKeySpec(this.data));
            }
            if (this.keyPair == 'private') {
                return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(this.data));
            }
            throw new Error()
        }
        return new SecretKeySpec(this.data, algorithm);
    }

    Key.prototype.toString = function () {
        let data = Base64.encodeToString(this.data, Base64.NO_WRAP);
        if (this.keyPair) {
            return "Key[" + this.keyPair + "]{data='" + data + "'}";
        }
        return "Key{data='" + data + "'}";
    }

    function KeyPair(publicKey, privateKey, options) {
        let options = Object.assign({}, options || {});
        options.keyPair = 'public';
        this.publicKey = new Key(publicKey, options);
        options.keyPair = 'private';
        this.privateKey = new Key(privateKey, options);
    }

    return $crypto;
}