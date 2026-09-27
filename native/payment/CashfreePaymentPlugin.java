package com.gyanverse.roamwise.payment;

import com.cashfree.pg.api.CFPaymentGatewayService;
import com.cashfree.pg.core.api.CFSession;
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback;
import com.cashfree.pg.core.api.exception.CFException;
import com.cashfree.pg.core.api.utils.CFErrorResponse;
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment;
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutTheme;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * Opens Cashfree through its official Android SDK. Bundled Capacitor pages use
 * https://localhost, which cannot be approved as a live Cashfree web domain.
 * This bridge binds checkout to the verified Play app instead. It deliberately
 * does not grant purchases: JavaScript still asks the authenticated Worker to
 * verify order_status=PAID and persist the account entitlement.
 */
@CapacitorPlugin(name = "CashfreePayment")
public class CashfreePaymentPlugin extends Plugin implements CFCheckoutResponseCallback {
    private String activeOrderId;

    @Override
    public void load() {
        super.load();
        registerCallback();
    }

    private void registerCallback() {
        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this);
        } catch (CFException ignored) {
            // checkout() retries and returns a useful error to the web layer.
        }
    }

    @PluginMethod
    public synchronized void checkout(PluginCall call) {
        final String paymentSessionId = call.getString("paymentSessionId", "").trim();
        final String orderId = call.getString("orderId", "").trim();
        final String environment = call.getString("environment", "sandbox");
        if (paymentSessionId.isEmpty() || orderId.isEmpty()) {
            call.reject("Cashfree session or order ID is missing.");
            return;
        }
        if (activeOrderId != null) {
            call.reject("A Cashfree checkout is already open.");
            return;
        }
        activeOrderId = orderId;
        getActivity().runOnUiThread(() -> {
            try {
                registerCallback();
                CFSession.Environment cfEnvironment = "production".equalsIgnoreCase(environment)
                    ? CFSession.Environment.PRODUCTION : CFSession.Environment.SANDBOX;
                CFSession session = new CFSession.CFSessionBuilder()
                    .setEnvironment(cfEnvironment)
                    .setPaymentSessionID(paymentSessionId)
                    .setOrderId(orderId)
                    .build();
                CFWebCheckoutTheme theme = new CFWebCheckoutTheme.CFWebCheckoutThemeBuilder()
                    .setNavigationBarBackgroundColor("#07090F")
                    .setNavigationBarTextColor("#FFFFFF")
                    .build();
                CFWebCheckoutPayment payment = new CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                    .setSession(session)
                    .setCFWebCheckoutUITheme(theme)
                    .build();
                CFPaymentGatewayService.getInstance().doPayment(getActivity(), payment);
                JSObject result = new JSObject();
                result.put("started", true);
                result.put("orderId", orderId);
                call.resolve(result);
            } catch (CFException | RuntimeException error) {
                synchronized (CashfreePaymentPlugin.this) { activeOrderId = null; }
                call.reject("Cashfree checkout could not start: " + safeMessage(error), null, error);
            }
        });
    }

    @Override
    public synchronized void onPaymentVerify(String orderId) {
        activeOrderId = null;
        JSObject event = new JSObject();
        event.put("orderId", orderId);
        notifyListeners("paymentVerify", event, true);
    }

    @Override
    public synchronized void onPaymentFailure(CFErrorResponse error, String orderId) {
        activeOrderId = null;
        JSObject event = new JSObject();
        event.put("orderId", orderId);
        event.put("message", error == null ? "Payment was cancelled or failed." : safeMessage(error));
        notifyListeners("paymentFailure", event, true);
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.trim().isEmpty() ? "Unknown Cashfree error" : message;
    }

    private static String safeMessage(CFErrorResponse error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.trim().isEmpty() ? "Payment was cancelled or failed." : message;
    }
}
