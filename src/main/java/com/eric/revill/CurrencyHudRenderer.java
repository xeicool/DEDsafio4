package com.eric.revill.client.render;

/** Saldo de deditas en el cliente. TODO: dibujar el HUD (etapa cliente). */
public final class CurrencyHudRenderer {
    private static int blue;
    private static int red;
    private static int green;

    private CurrencyHudRenderer() {}

    public static void setBalances(int blueAmount, int redAmount, int greenAmount) {
        blue = blueAmount;
        red = redAmount;
        green = greenAmount;
    }

    /** Orden: azul, roja, verde (mismo orden que CurrencyType). */
    public static int[] getBalances() {
        return new int[] {blue, red, green};
    }
}
