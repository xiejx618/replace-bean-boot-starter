package io.github.xiejx618.replace;

public class ReplaceProperties {
    private boolean enabled = true;
    private String packages;
    private String factories;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPackages() {
        return packages;
    }

    public void setPackages(String packages) {
        this.packages = packages;
    }

    public String getFactories() {
        return factories;
    }

    public void setFactories(String factories) {
        this.factories = factories;
    }
}
