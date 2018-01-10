
package in.gauravlanjekar.awscodegen;


public class ResponseParameters {

    private String methodResponseHeaderAccessControlAllowMethods;
    private String methodResponseHeaderAccessControlAllowHeaders;
    private String methodResponseHeaderAccessControlAllowOrigin;

    public String getMethodResponseHeaderAccessControlAllowMethods() {
        return methodResponseHeaderAccessControlAllowMethods;
    }

    public void setMethodResponseHeaderAccessControlAllowMethods(String methodResponseHeaderAccessControlAllowMethods) {
        this.methodResponseHeaderAccessControlAllowMethods = methodResponseHeaderAccessControlAllowMethods;
    }

    public String getMethodResponseHeaderAccessControlAllowHeaders() {
        return methodResponseHeaderAccessControlAllowHeaders;
    }

    public void setMethodResponseHeaderAccessControlAllowHeaders(String methodResponseHeaderAccessControlAllowHeaders) {
        this.methodResponseHeaderAccessControlAllowHeaders = methodResponseHeaderAccessControlAllowHeaders;
    }

    public String getMethodResponseHeaderAccessControlAllowOrigin() {
        return methodResponseHeaderAccessControlAllowOrigin;
    }

    public void setMethodResponseHeaderAccessControlAllowOrigin(String methodResponseHeaderAccessControlAllowOrigin) {
        this.methodResponseHeaderAccessControlAllowOrigin = methodResponseHeaderAccessControlAllowOrigin;
    }

}
