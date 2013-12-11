package android.lib.iab;

public interface OnInitListener {
    void onInitReady();

    void onInitCompleted(int status);
}
