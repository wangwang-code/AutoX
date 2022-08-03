module.exports = function (runtime, scope) {
    var okhttp3 = Packages["okhttp3"];
    var Callback = okhttp3.Callback;
    var FormBody = okhttp3.FormBody;
    var MutableOkHttp = com.stardust.autojs.core.http.MutableOkHttp;
    var MultipartBody = okhttp3.MultipartBody;
    var MediaType = okhttp3.MediaType;
    var Request = okhttp3.Request;
    var RequestBody = okhttp3.RequestBody;
    var http = {};
    var $files = scope.$files;

    http.__okhttp__ = new MutableOkHttp();

    http.buildRequest = function (url, options) {
        var r = new Request.Builder();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        r.url(url);
        if (options.headers) {
            setHeaders(r, options.headers);
        }
        if (options.body) {
            r.method(options.method, parseBody(options, options.body));
        } else if (options.files) {
            r.method(options.method, parseMultipart(options.files));
        } else {
            r.method(options.method, null);
        }
        return r.build();
    }

    http.client = function () {
        return http.__okhttp__.client();
    }

    http.get = function (url, options, callback) {
        options = options || {};
        options.method = "GET";
        return http.request(url, options, callback);
    }

    http.post = function (url, data, options, callback) {
        options = options || {};
        options.method = "POST";
        options.contentType = options.contentType || "application/x-www-form-urlencoded";
        if (data) {
            fillPostData(options, data);
        }
        return http.request(url, options, callback);
    }

    http.postJson = function (url, data, options, callback) {
        options = options || {};
        options.contentType = "application/json";
        return http.post(url, data, options, callback);
    }

    http.postMultipart = function (url, files, options, callback) {
        options = options || {};
        options.method = "POST";
        options.contentType = "multipart/form-data";
        options.files = files;
        return http.request(url, options, callback);a
    }

    http.request = function (url, options, callback) {
        var cont = null;
        var disposable = null;
        if (!callback && ui.isUiThread() && continuation.enabled) {
            cont = continuation.create();
        }
        var call = http.client().newCall(http.buildRequest(url, options));
        if (!callback && !cont) {
            disposable = threads.disposable();
            callback = function (res, ex) {
                disposable.setAndNotify({
                    error: ex,
                    response: res
                });
            }
        }
        call.enqueue(new Callback({
            onResponse: function (call, res) {
                res = new HttpResponse(res);
                cont && cont.resume(res);
                callback && callback(res);
            },
            onFailure: function (call, ex) {
                cont && cont.resumeError(ex);
                callback && callback(null, ex);
            }
        }));
        if (cont) {
            return cont.await();
        }
        if (disposable) {
            try {
                var result = disposable.blockedGet(http.__okhttp__.timeout);
                if (result.error) {
                    throw result.error;
                }
                return result.response;
            } catch (e) {
                call.cancel();
                throw e;
            }
        }

    }

    function fillPostData(options, data) {
        if (options.contentType == "application/x-www-form-urlencoded") {
            var b = new FormBody.Builder();
            for (var key in data) {
                if (data.hasOwnProperty(key)) {
                    let value = data[key];
                    if (value == null) {
                        throw new Error("Post data value with key '" + key + "'is null");
                    }
                    b.add(key, data[key]);
                }
            }
            options.body = b.build();
        } else if (options.contentType == "application/json") {
            options.body = JSON.stringify(data);
        } else {
            options.body = data;
        }
    }

    function HttpResponse(res) {
        this.raw = res;
        this.statusCode = res.code();
        this.statusMessage = res.message();
        this.body = new HttpResponseBody(this);
        this.request = res.request();
        this.url = this.request.url();
        this.method = this.request.method();

        let headers = res.headers();
        this.headers = {};
        for (var i = 0; i < headers.size(); i++) {
            let name = headers.name(i);
            let value = headers.value(i);
            if (this.headers.hasOwnProperty(name)) {
                let origin = this.headers[name];
                if (!Array.isArray(origin)) {
                    this.headers[name] = [origin];
                }
                this.headers[name].push(value);
            } else {
                this.headers[name] = value;
            }
        }

    }

    function HttpResponseBody(res) {
        this.response = res;
        this.raw = res.raw.body();
        this.contentType = this.raw.contentType();
    }

    function parseMultipart(files) {
        var builder = new MultipartBody.Builder()
            .setType(MultipartBody.FORM);
        for (var key in files) {
            if (!files.hasOwnProperty(key)) {
                continue;
            }
            var value = files[key];
            if (typeof (value) == 'string') {
                builder.addFormDataPart(key, value);
                continue;
            }
            var path, mimeType, fileName;
            if (typeof (value.getPath) == 'function') {
                path = value.getPath();
            } else if (value.length == 2) {
                fileName = value[0];
                path = value[1];
            } else if (value.length >= 3) {
                fileName = value[0];
                mimeType = value[1]
                path = value[2];
            } else {
                builder.addFormDataPart(key, value);
                continue;
//                throw new Error('Cannot parse multipart data: key = ' + key + ', value = ' + value + ', typeof value = ' + typeof(value));
            }
            var file = new java.io.File($files.path(path));
            fileName = fileName || file.getName();
            mimeType = mimeType || parseMimeType($files.getExtension(fileName));
            builder.addFormDataPart(key, fileName, RequestBody.create(MediaType.parse(mimeType), file));
        }
        return builder.build();
    }

    function parseMimeType(ext) {
        if (ext.length == 0) {
            return "application/octet-stream";
        }
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            || "application/octet-stream";
    }

    function parseBody(options, body) {
        if (typeof (body) == "string") {
            body = RequestBody.create(MediaType.parse(options.contentType), body);
        } else if (body instanceof RequestBody) {
            return body;
        } else {
            body = new RequestBody({
                contentType: function () {
                    return MediaType.parse(options.contentType);
                },
                writeTo: body
            });
        }
        return body;
    }

    function setHeaders(r, headers) {
         for (var key in headers) {
             if (headers.hasOwnProperty(key)) {
                 let value = headers[key];
                 if (Array.isArray(value)) {
                     value.forEach(v => {
                         r.header(key, v);
                     });
                 } else {
                     r.header(key, value);
                 }
             }
         }
     }

    HttpResponseBody.prototype.string = function () {
        if (typeof (this.__string__) === 'undefined') {
            this.__string__ = this.raw.string();
        }
        return this.__string__;
    }

    HttpResponseBody.prototype.json = function () {
        return JSON.parse(this.string());
    }

    HttpResponseBody.prototype.bytes = function () {
        return this.raw.bytes();
    }

    return http;
}