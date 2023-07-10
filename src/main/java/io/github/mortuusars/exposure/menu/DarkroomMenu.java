package io.github.mortuusars.exposure.menu;

import io.github.mortuusars.exposure.Exposure;
import io.github.mortuusars.exposure.block.entity.DarkroomBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DarkroomMenu extends AbstractContainerMenu {
    private final DarkroomBlockEntity darkroomBlockEntity;
    private final ContainerData data;
    public DarkroomMenu(int containerId, final Inventory playerInventory, final DarkroomBlockEntity blockEntity, ContainerData containerData) {
        super(Exposure.MenuTypes.DARKROOM.get(), containerId);
        this.darkroomBlockEntity = blockEntity;
        this.data = containerData;

        IItemHandler itemHandler = blockEntity.getInventory();
        {
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.FILM_SLOT, 14, 47));
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.CYAN_DYE_SLOT, 8, 96));
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.MAGENTA_DYE_SLOT, 26, 96));
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.YELLOW_DYE_SLOT, 44, 96));
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.BLACK_DYE_SLOT, 62, 96));
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.PAPER_SLOT, 81, 96));

            // OUTPUT
            this.addSlot(new SlotItemHandler(itemHandler, DarkroomBlockEntity.RESULT_SLOT, 152, 96) {
                @Override
                public boolean mayPlace(@NotNull ItemStack stack) {
                    return false;
                }
            });

        }

        // Player inventory slots
        for(int row = 0; row < 3; ++row) {
            for(int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 128 + row * 18));
            }
        }

        // Player hotbar slots
        // Hotbar should go after main inventory for Shift+Click to work properly.
        for(int index = 0; index < 9; ++index) {
            this.addSlot(new Slot(playerInventory, index, 8 + index * 18, 186));
        }

        this.addDataSlots(data);
    }

    public static DarkroomMenu fromBuffer(int containerID, Inventory playerInventory, FriendlyByteBuf buffer) {
        return new DarkroomMenu(containerID, playerInventory, getBlockEntity(playerInventory, buffer),
                new SimpleContainerData(DarkroomBlockEntity.CONTAINER_DATA_SIZE));
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        ItemStack clickedStack = slot.getItem();

        if (index < DarkroomBlockEntity.SLOTS) {
            if (!moveItemStackTo(clickedStack, DarkroomBlockEntity.SLOTS, slots.size(), true))
                return ItemStack.EMPTY;

            // BEs inventory onContentsChanged is not fired when removing agreement by shift clicking.
            // So we force update the slot.
            // This is needed to update agreement-related stuff. (Blockstate was not updating properly to reflect the removal).
//            if (index == DeliveryTableBlockEntity.AGREEMENT_SLOT)
//                blockEntity.setItem(DeliveryTableBlockEntity.AGREEMENT_SLOT, slot.getItem());
        }
        else if (index < slots.size()) {
            if (!moveItemStackTo(clickedStack, 0, DarkroomBlockEntity.SLOTS, false))
                return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return darkroomBlockEntity.stillValid(player);
    }

    private static DarkroomBlockEntity getBlockEntity(final Inventory playerInventory, final FriendlyByteBuf data) {
        Objects.requireNonNull(playerInventory, "playerInventory cannot be null");
        Objects.requireNonNull(data, "data cannot be null");
        final BlockEntity blockEntityAtPos = playerInventory.player.level.getBlockEntity(data.readBlockPos());
        if (blockEntityAtPos instanceof DarkroomBlockEntity blockEntity)
            return blockEntity;
        throw new IllegalStateException("Block entity is not correct! " + blockEntityAtPos);
    }
}
