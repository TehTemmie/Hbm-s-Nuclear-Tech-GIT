package com.hbm.items.machine;

import java.util.List;

import com.hbm.items.ItemEnumMulti;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import com.hbm.render.icon.RGBMutatorInterpolatedComponentRemap;
import com.hbm.render.icon.TextureAtlasSpriteMutatable;
import com.hbm.util.EnumUtil;
import com.hbm.util.function.Function;
import com.hbm.util.function.Function.*;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/*
 * Watz Isotropic Fuel, Oxidized
 */
public class ItemWatzPellet extends ItemEnumMulti {

	public ItemWatzPellet() {
		super(EnumWatzType.class, true, true);
		this.setMaxStackSize(16);
		this.setCreativeTab(MainRegistry.controlTab);
	}

	public static enum EnumWatzType {

		//TODO: durability
		SCHRABIDIUM(	0x32FFFF, 0x005C5C, 2_000,	10D,	new FunctionLogarithmic(10), null, null),
		HES(			0x66DCD6, 0x023933, 1_500,	10D,	null, null, null),
		LES(			0xABB4A8, 0x0C1105, 500,	10D,	null, null, null),
		MES(			0xCBEADF, 0x28473C, 1_000,	10D,	null, null, null),
		NP(				0xA6B2A6, 0x030F03, 0,		10D,	null, null, null),
		MEU(			0xC1C7BD, 0x2B3227, 0,		10D,	null, null, null),
		MEP(			0x9AA3A0, 0x111A17, 0,		10D,	null, null, null),
		LEAD(			0xA6A6B2, 0x03030F, 0,		0,		null, null, new FunctionSqrt(10)), //standard absorber, negative coefficient
		DU(				0xC1C7BD, 0x2B3227, 0,		0, 		null, null, new FunctionQuadratic(1D, 1D).withDiv(100)); //absorber with positive coefficient 
		
		public double yield = 1_000_000_000;
		public int colorLight;
		public int colorDark;
		public double mudContent;	//how much mud per reaction flux should be produced
		public double passive;		//base flux emission
		public double heatEmission;	//reactivity(1) to heat (heat per outgoing flux)
		public Function burnFunc;	//flux to reactivity(0) (classic reactivity)
		public Function heatMult;	//reactivity(0) to reactivity(1) based on heat (temperature coefficient)
		public Function absorbFunc;	//flux to heat (flux absobtion for non-active component)
		
		private EnumWatzType(int colorLight, int colorDark, double passive, double heatEmission, Function burnFunction, Function heatMultiplier, Function absorbFunction) {
			this.colorLight = colorLight;
			this.colorDark = colorDark;
			this.passive = passive;
			this.heatEmission = heatEmission;
			this.burnFunc = burnFunction;
			this.heatMult = heatMultiplier;
			this.absorbFunc = absorbFunction;
		}
	}

	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		
		Enum[] enums = theEnum.getEnumConstants();
		this.icons = new IIcon[enums.length];
		
		if(reg instanceof TextureMap) {
			TextureMap map = (TextureMap) reg;
			
			for(int i = 0; i < EnumWatzType.values().length; i++) {
				EnumWatzType type = EnumWatzType.values()[i];
				String placeholderName = this.getIconString() + "-" + (type.name() + this.getUnlocalizedName());
				int light = this == ModItems.watz_pellet_depleted ? desaturate(type.colorLight) : type.colorLight;
				int dark = this == ModItems.watz_pellet_depleted ? desaturate(type.colorDark) : type.colorDark;
				TextureAtlasSpriteMutatable mutableIcon = new TextureAtlasSpriteMutatable(placeholderName, new RGBMutatorInterpolatedComponentRemap(0xD2D2D2, 0x333333, light, dark));
				map.setTextureEntry(placeholderName, mutableIcon);
				icons[i] = mutableIcon;
			}
		}
		
		this.itemIcon = reg.registerIcon(this.getIconString());
	}
	
	public static int desaturate(int color) {
		int r = (color & 0xff0000) >> 16;
		int g = (color & 0x00ff00) >> 8;
		int b = (color & 0x0000ff);
		
		int avg = (r + g + b) / 3;
		double approach = 0.9;
		double mult = 0.75;

		r -= (r - avg) * approach;
		g -= (g - avg) * approach;
		b -= (b - avg) * approach;

		r *= mult;
		g *= mult;
		b *= mult;
		
		return (r << 16) | (g << 8) | b;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int meta) {
		IIcon icon = super.getIconFromDamage(meta);
		return icon == null ? this.itemIcon : icon; //fallback if TextureMap fails during register
	}
	
	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		EnumWatzType num = EnumUtil.grabEnumSafely(EnumWatzType.class, stack.getItemDamage());
		
		String color = EnumChatFormatting.GOLD + "";
		String reset = EnumChatFormatting.RESET + "";

		if(num.passive > 0) list.add(color + "Base fission rate: " + reset + num.passive);
		if(num.heatEmission > 0) list.add(color + "Heat per flux: " + reset + num.heatEmission + " TU");
		if(num.burnFunc != null) {
			list.add(color + "Reacton function: " + reset + num.burnFunc.getLabelForFuel());
			list.add(color + "Fuel type: " + reset + num.burnFunc.getDangerFromFuel());
		}
		if(num.heatMult != null) list.add(color + "Thermal coefficient: " + reset + num.heatMult.getLabelForFuel());
		if(num.absorbFunc != null) list.add(color + "Flux capture: " + reset + num.absorbFunc.getLabelForFuel());
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return getDurabilityForDisplay(stack) > 0D;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return 1D - getEnrichment(stack);
	}
	
	public static double getEnrichment(ItemStack stack) {
		EnumWatzType num = EnumUtil.grabEnumSafely(EnumWatzType.class, stack.getItemDamage());
		return getYield(stack) / num.yield;
	}
	
	public static double getYield(ItemStack stack) {
		return getDouble(stack, "yield");
	}
	
	public static void setYield(ItemStack stack, double yield) {
		setDouble(stack, "yield", yield);
	}
	
	public static void setDouble(ItemStack stack, String key, double yield) {
		if(!stack.hasTagCompound()) setNBTDefaults(stack);
		stack.stackTagCompound.setDouble(key, yield);
	}
	
	public static double getDouble(ItemStack stack, String key) {
		if(!stack.hasTagCompound()) setNBTDefaults(stack);
		return stack.stackTagCompound.getDouble(key);
	}
	
	private static void setNBTDefaults(ItemStack stack) {
		EnumWatzType num = EnumUtil.grabEnumSafely(EnumWatzType.class, stack.getItemDamage());
		stack.stackTagCompound = new NBTTagCompound();
		setYield(stack, num.yield);
	}
	
	@Override
	public void onCreated(ItemStack stack, World world, EntityPlayer player) {
		setNBTDefaults(stack); //minimize the window where NBT screwups can happen
	}
}
