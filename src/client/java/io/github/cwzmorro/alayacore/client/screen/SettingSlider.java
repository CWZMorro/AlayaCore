package io.github.cwzmorro.alayacore.client.screen;

import io.github.cwzmorro.alayacore.util.Steps;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

final class SettingSlider extends AbstractSliderButton {
	private final double min;
	private final double max;
	private final double step;
	private final DoubleFunction<Component> label;
	private final DoubleConsumer apply;

	SettingSlider(int x, int y, int width, double min, double max, double step, double current, DoubleFunction<Component> label, DoubleConsumer apply) {
		super(x, y, width, DEFAULT_HEIGHT, label.apply(current), (current - min) / (max - min));
		this.min = min;
		this.max = max;
		this.step = step;
		this.label = label;
		this.apply = apply;
	}

	private double chosen() {
		return Steps.snap(this.min, this.max, this.step, this.min + this.value * (this.max - this.min));
	}

	@Override
	protected void setValue(double newValue) {
		double range = this.max - this.min;
		super.setValue((Steps.snap(this.min, this.max, this.step, this.min + newValue * range) - this.min) / range);
	}

	@Override
	protected void updateMessage() {
		this.setMessage(this.label.apply(this.chosen()));
	}

	@Override
	protected void applyValue() {
		this.apply.accept(this.chosen());
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (this.canChangeValue && (event.isLeft() || event.isRight())) {
			this.setValue(this.value + (event.isLeft() ? -1 : 1) * this.step / (this.max - this.min));
			return true;
		}
		return super.keyPressed(event);
	}
}
