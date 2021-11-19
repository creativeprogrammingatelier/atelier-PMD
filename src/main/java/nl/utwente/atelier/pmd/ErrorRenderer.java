package nl.utwente.atelier.pmd;

import net.sourceforge.pmd.renderers.Renderer;

public interface ErrorRenderer extends Renderer {
    void renderError(String errorMessage);
}
