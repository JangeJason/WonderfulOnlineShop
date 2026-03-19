import { createOrderPayment, syncOrderPayment } from "../api/order";

function wait(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms));
}

async function pollUntilPaid(orderId: number, maxRounds = 40, intervalMs = 3000) {
  for (let i = 0; i < maxRounds; i += 1) {
    try {
      const order = await syncOrderPayment(orderId);
      if (order.status !== "PENDING") {
        return true;
      }
    } catch {
      // ignore one-off poll errors
    }
    await wait(intervalMs);
  }
  return false;
}

export async function triggerWechatPay(orderId: number): Promise<void> {
  // #ifdef MP-WEIXIN
  const loginRes: any = await uni.login({ provider: "weixin" });
  const jsCode = loginRes?.code;
  if (!jsCode) {
    throw new Error("无法获取微信登录凭证，请重试");
  }
  const payDataMp = await createOrderPayment(orderId, {
    channel: "JSAPI",
    jsCode,
  });
  const params = payDataMp.jsapiParams;
  if (!params) {
    throw new Error("支付参数缺失");
  }
  await new Promise<void>((resolve, reject) => {
    uni.requestPayment({
      provider: "wxpay",
      timeStamp: params.timeStamp,
      nonceStr: params.nonceStr,
      package: params.packageValue,
      signType: params.signType,
      paySign: params.paySign,
      success: () => resolve(),
      fail: (err) => reject(new Error((err as any)?.errMsg || "支付未完成")),
    } as any);
  });

  const paidMp = await pollUntilPaid(orderId, 12, 1500);
  if (!paidMp) {
    throw new Error("支付结果确认中，请稍后刷新订单");
  }
  return;
  // #endif

  // #ifdef H5
  const payDataH5 = await createOrderPayment(orderId, {
    channel: "NATIVE",
  });
  if (!payDataH5.codeUrl) {
    throw new Error("支付二维码生成失败");
  }
  const qrImageUrl =
    "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=" +
    encodeURIComponent(payDataH5.codeUrl);
  if (typeof window !== "undefined") {
    window.open(qrImageUrl, "_blank");
  }
  uni.showModal({
    title: "请扫码支付",
    content: "已打开支付二维码新窗口，请使用微信扫码完成支付。",
    showCancel: false,
  });
  const paidH5 = await pollUntilPaid(orderId);
  if (!paidH5) {
    throw new Error("未检测到支付成功，请稍后在订单页重试");
  }
  return;
  // #endif

  throw new Error("当前端暂不支持微信支付");
}
