package ganymedes01.etfuturum.items;

import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.item.ItemRecord;
import net.minecraft.util.ResourceLocation;

public class ItemEtFuturumRecord extends ItemRecord {

	public ItemEtFuturumRecord(String recordName) {
		super(recordName);
		setTextureName("music_disc_" + recordName);
		setUnlocalizedName("record");
	}

	@Override
	public ResourceLocation getRecordResource(String name) {
		return new ResourceLocation(Reference.MCAssetVer + ":music_disc." + recordName);
	}

}
