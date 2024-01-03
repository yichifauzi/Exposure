package io.github.mortuusars.exposure.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.item.PhotographItem;
import io.github.mortuusars.exposure.menu.AlbumMenu;
import io.github.mortuusars.exposure.menu.AlbumPlayerInventorySlot;
import io.github.mortuusars.exposure.util.PagingDirection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public class AlbumScreen extends AbstractContainerScreen<AlbumMenu> {
    private class Page {
        public final AlbumMenu.Page page;
        public final Rect2i photoArea;
        public final Rect2i exposureArea;
        public final Button addPhotoButton;
        public final int photoButtonId;

        public Page(@NotNull AlbumMenu.Page page, Rect2i photoArea, Rect2i exposureArea, @NotNull Button addPhotoButton, int photoButtonId) {
            this.page = page;
            this.photoArea = photoArea;
            this.exposureArea = exposureArea;
            this.addPhotoButton = addPhotoButton;
            this.photoButtonId = photoButtonId;
        }

        public boolean isMouseOver(double mouseX, double mouseY) {
            return isHovering(photoArea.getX() - leftPos, photoArea.getY() - topPos,
                    photoArea.getWidth(), photoArea.getHeight(), mouseX, mouseY);
        }
    }

    public static final ResourceLocation TEXTURE = Exposure.resource("textures/gui/album.png");
    public static final int MAIN_FONT_COLOR = 0xB59774;
    public static final int SECONDARY_FONT_COLOR = 0xEFE4CA;

    @NotNull
    private final Minecraft minecraft;
    @NotNull
    private final Player player;
    @NotNull
    private final MultiPlayerGameMode gameMode;

    private final Pager pager = new Pager(TEXTURE) {
        @Override
        public void init(int screenWidth, int screenHeight, int pages, boolean cycled, Consumer<AbstractButton> addButtonAction) {
            this.pages = pages;
            this.cycled = cycled;
            previousButton = new ImageButton(leftPos + 12, topPos + 164, 13, 15,
                    149, 188, 15, texture, 512, 512, button -> onPreviousButtonPressed());
            nextButton = new ImageButton(leftPos + 274, topPos + 164, 13, 15,
                    162, 188, 15, texture, 512, 512, button -> onNextButtonPressed());

            previousButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.album.previous_page")));
            nextButton.setTooltip(Tooltip.create(Component.translatable("gui.exposure.album.next_page")));

            addButtonAction.accept(previousButton);
            addButtonAction.accept(nextButton);

            update();
        }

        @Override
        public void onPageChanged(PagingDirection pagingDirection, int prevPage, int currentPage) {
            super.onPageChanged(pagingDirection, prevPage, currentPage);
            pressButton(pagingDirection == PagingDirection.PREVIOUS ? AlbumMenu.PREVIOUS_PAGE_BUTTON : AlbumMenu.NEXT_PAGE_BUTTON);
        }

        @Override
        protected SoundEvent getChangeSound() {
            return SoundEvents.BOOK_PAGE_TURN;
        }
    };

    private final List<Page> pages = new ArrayList<>();

    public AlbumScreen(AlbumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        minecraft = Minecraft.getInstance();
        player = Objects.requireNonNull(minecraft.player);
        gameMode = Objects.requireNonNull(minecraft.gameMode);

    }

    @Override
    public void added() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(Exposure.SoundEvents.PHOTOGRAPH_RUSTLE.get(), 1f));
    }

    @Override
    protected void init() {
        this.imageWidth = 299;
        this.imageHeight = 188;
        super.init();

        titleLabelY = -999;
        inventoryLabelX = 69;
        inventoryLabelY = -999;

        int spreadsCount = (int) Math.ceil(getMenu().getPages().size() / 2f);
        pager.init(width, height, spreadsCount, false, this::addRenderableWidget);

        pages.clear();

        MutableComponent addButtonTooltip = Component.translatable("gui.exposure.album.add_photograph");

        Rect2i leftPhotoArea = new Rect2i(leftPos + 25, topPos + 21, 108, 109);
        Rect2i leftPhotoExposure = new Rect2i(this.leftPos + 31, this.topPos + 27, 96, 96);

        Button leftAddPhotoButton = new ImageButton(leftPhotoArea.getX(), leftPhotoArea.getY(),
                leftPhotoArea.getWidth(), leftPhotoArea.getHeight(), 299, 0, 109,
                TEXTURE, 512, 512, this::onButtonPress);
        leftAddPhotoButton.setTooltip(Tooltip.create(addButtonTooltip));

        pages.add(new Page(AlbumMenu.Page.LEFT, leftPhotoArea, leftPhotoExposure,
                leftAddPhotoButton, AlbumMenu.LEFT_PAGE_PHOTO_BUTTON));
        addRenderableWidget(leftAddPhotoButton);

        Rect2i rightPhotoArea = new Rect2i(leftPos + 166, topPos + 21, 108, 109);
        Rect2i rightPhotoExposure = new Rect2i(this.leftPos + 172, this.topPos + 27, 96, 96);

        Button rightAddPhotoButton = new ImageButton(rightPhotoArea.getX(), rightPhotoArea.getY(),
                rightPhotoArea.getWidth(), rightPhotoArea.getHeight(), 299, 0, 109,
                TEXTURE, 512, 512, this::onButtonPress);
        rightAddPhotoButton.setTooltip(Tooltip.create(addButtonTooltip));

        pages.add(new Page(AlbumMenu.Page.RIGHT, rightPhotoArea, rightPhotoExposure,
                rightAddPhotoButton, AlbumMenu.RIGHT_PAGE_PHOTO_BUTTON));
        addRenderableWidget(rightAddPhotoButton);

        for (Page page : pages) {
            page.addPhotoButton.visible = getMenu().isAlbumEditable();
            page.addPhotoButton.active = getMenu().isAlbumEditable();
        }
    }

    private void onButtonPress(Button button) {
        for (Page pageData : pages) {
            if (button == pageData.addPhotoButton)
                pressButton(pageData.photoButtonId);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        pager.update();

        boolean isInAddingPhotographMode = getMenu().isInAddingPhotographMode();

        inventoryLabelY = isInAddingPhotographMode ? getMenu().getPlayerInventorySlots().get(0).y - 12 : -999;

        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isInAddingPhotographMode) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            for (Slot slot : getMenu().slots) {
                if (!slot.getItem().isEmpty() && !(slot.getItem().getItem() instanceof PhotographItem)) {
                    guiGraphics.blit(TEXTURE, leftPos + slot.x - 1, topPos + slot.y - 1, 350, 176, 376,
                            18, 18, 512, 512);
                }
            }
            RenderSystem.disableBlend();
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 15);
        super.renderLabels(guiGraphics, mouseX, mouseY);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (getMenu().isInAddingPhotographMode() && hoveredSlot != null && !hoveredSlot.getItem()
                .isEmpty() && !(hoveredSlot.getItem().getItem() instanceof PhotographItem))
            return;

        if (!getMenu().isInAddingPhotographMode()) {
            for (Page page : pages) {
                if (!page.addPhotoButton.visible && page.isMouseOver(x, y)) {
                    getMenu().getPhotographSlot(page.page).ifPresent(slot -> {
                        ItemStack stack = slot.getItem();
                        List<Component> tooltip = this.getTooltipFromContainerItem(stack);
                        tooltip.add(Component.empty());
                        tooltip.add(Component.translatable("gui.exposure.album.left_click_to_view"));
                        tooltip.add(Component.translatable("gui.exposure.album.right_click_to_remove"));
                        guiGraphics.renderTooltip(this.font, tooltip,
                                (stack.getItem() instanceof PhotographItem ? Optional.empty() : stack.getTooltipImage()), x, y);
                    });

                    return;
                }
            }
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> tooltipLines = super.getTooltipFromContainerItem(stack);
        if (getMenu().isInAddingPhotographMode() && hoveredSlot != null && hoveredSlot.getItem() == stack
                && stack.getItem() instanceof PhotographItem) {
            tooltipLines.add(Component.empty());
            tooltipLines.add(Component.translatable("gui.exposure.album.left_click_to_add")
                    .withStyle(ChatFormatting.GOLD));
        }
        return tooltipLines;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, 0,
                this.imageWidth, this.imageHeight, 512, 512);

        int currentSpreadIndex = getMenu().getCurrentSpreadIndex();
        drawPageNumbers(guiGraphics, currentSpreadIndex);

        for (Page page : pages) {
            getMenu().getPhotographSlot(page.page).ifPresent(slot -> {
                ItemStack photoStack = slot.getItem();

                page.addPhotoButton.visible = getMenu().isAlbumEditable() && !getMenu().isInAddingPhotographMode() && photoStack.isEmpty();
                page.addPhotoButton.active = getMenu().isAlbumEditable() && !getMenu().isInAddingPhotographMode() && photoStack.isEmpty();

                if (photoStack.getItem() instanceof PhotographItem photographItem) {
                    Rect2i area = page.photoArea;
                    guiGraphics.blit(TEXTURE, area.getX(), area.getY(), 0, 299, page.isMouseOver(mouseX, mouseY) ? 327 : 218,
                            area.getWidth(), area.getHeight(), 512, 512);

                    @Nullable Either<String, ResourceLocation> idOrTexture = photographItem.getIdOrTexture(photoStack);
                    if (idOrTexture != null) {
                        Rect2i expArea = page.exposureArea;
                        ExposureClient.getExposureRenderer().renderSimple(idOrTexture, guiGraphics.pose(),
                                expArea.getX(), expArea.getY(), expArea.getWidth(), expArea.getHeight());
                    }
                }
            });
        }

        if (getMenu().isInAddingPhotographMode()) {
            @Nullable AlbumMenu.Page pageBeingAddedTo = getMenu().getPageBeingAddedTo();
            for (Page page : pages) {
                if (page.page == pageBeingAddedTo) {
                    guiGraphics.blit(TEXTURE, page.photoArea.getX(), page.photoArea.getY(), 5, 299, 109,
                            page.photoArea.getWidth(), page.photoArea.getHeight(), 512, 512);
                    break;
                }
            }

            AlbumPlayerInventorySlot firstSlot = getMenu().getPlayerInventorySlots().get(0);
            int x = firstSlot.x - 8;
            int y = firstSlot.y - 18;
            guiGraphics.blit(TEXTURE, leftPos + x, topPos + y, 10, 0, 376, 176, 100, 512, 512);
        }
    }

    protected void drawPageNumbers(GuiGraphics guiGraphics, int currentSpreadIndex) {
        Font font = minecraft.font;

        String leftPageNumber = Integer.toString(currentSpreadIndex * 2 + 1);
        String rightPageNumber = Integer.toString(currentSpreadIndex * 2 + 2);

        guiGraphics.drawString(font, leftPageNumber, leftPos + 71 + (8 - font.width(leftPageNumber) / 2),
                topPos + 167, SECONDARY_FONT_COLOR, false);

        guiGraphics.drawString(font, rightPageNumber, leftPos + 212 + (8 - font.width(rightPageNumber) / 2),
                topPos + 167, SECONDARY_FONT_COLOR, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!getMenu().isAlbumEditable())
            return super.mouseClicked(mouseX, mouseY, button);

        if (getMenu().isInAddingPhotographMode()) {
            AlbumPlayerInventorySlot firstSlot = getMenu().getPlayerInventorySlots().get(0);
            int x = firstSlot.x - 8;
            int y = firstSlot.y - 18;
            if (hoveredSlot == null) {
                if (isHovering(x, y, 188, 176, mouseX, mouseY))
                    return true;
                else if (!hasClickedOutside(mouseX, mouseY, leftPos, topPos, button)) {
                    pressButton(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
                    return true;
                }
            }
        } else if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
            for (Page page : pages) {
                if (page.isMouseOver(mouseX, mouseY)) {
                    pressButton(page.photoButtonId);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void pressButton(int buttonId) {
        getMenu().clickMenuButton(player, buttonId);
        gameMode.handleInventoryButtonClick(getMenu().containerId, buttonId);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot == null && getMenu().isInAddingPhotographMode()) {
            pressButton(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
            return;
        }

        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (getMenu().isInAddingPhotographMode() && (minecraft.options.keyInventory.matches(keyCode, scanCode)
                || keyCode == InputConstants.KEY_ESCAPE)) {
            pressButton(AlbumMenu.CANCEL_ADDING_PHOTO_BUTTON);
            return true;
        }

        return pager.handleKeyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return pager.handleKeyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }
}