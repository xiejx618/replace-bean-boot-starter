package io.github.xiejx618.replace;

/**
 * 替换配置类
 */
public class ReplaceProperties {
    /**
     * 是否启用
     */
    private boolean enabled = true;
    /**
     * 扫描包
     */
    private String packages;
    /**
     * 工厂类
     */
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
