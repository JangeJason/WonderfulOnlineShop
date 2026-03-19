package com.wonderful.onlineshop.order.dto;

public class OrderPayCreateResponse {

    private String channel;
    private String outTradeNo;
    private String codeUrl;
    private JsapiParams jsapiParams;

    public static class JsapiParams {
        private String timeStamp;
        private String nonceStr;
        private String packageValue;
        private String signType;
        private String paySign;

        public String getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(String timeStamp) {
            this.timeStamp = timeStamp;
        }

        public String getNonceStr() {
            return nonceStr;
        }

        public void setNonceStr(String nonceStr) {
            this.nonceStr = nonceStr;
        }

        public String getPackageValue() {
            return packageValue;
        }

        public void setPackageValue(String packageValue) {
            this.packageValue = packageValue;
        }

        public String getSignType() {
            return signType;
        }

        public void setSignType(String signType) {
            this.signType = signType;
        }

        public String getPaySign() {
            return paySign;
        }

        public void setPaySign(String paySign) {
            this.paySign = paySign;
        }
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getCodeUrl() {
        return codeUrl;
    }

    public void setCodeUrl(String codeUrl) {
        this.codeUrl = codeUrl;
    }

    public JsapiParams getJsapiParams() {
        return jsapiParams;
    }

    public void setJsapiParams(JsapiParams jsapiParams) {
        this.jsapiParams = jsapiParams;
    }
}
