package com.cyborgmas.mobstatues.data;

import com.cyborgmas.mobstatues.MobStatues;
import com.cyborgmas.mobstatues.registration.Registration;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.ModelFile.UncheckedModelFile;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

import static net.minecraftforge.client.model.generators.ModelBuilder.Perspective.*;

public class AllModelsGenerator extends BlockStateProvider {
    public AllModelsGenerator(GatherDataEvent event) {
        super(event.getGenerator(), MobStatues.MODID, event.getExistingFileHelper());
    }

    @Override
    protected void registerStatesAndModels() {
        this.itemModels().getBuilder("statue")
                .parent(new UncheckedModelFile("builtin/entity"))
                .transforms()
                .transform(GUI)
                .rotation(30, 45, 0).scale(0.625f).translation(7,0,0)
                .end()
                .transform(GROUND)
                .translation(0, 3,0).scale(0.25f)
                .end()
                .transform(HEAD)
                .rotation(0, 180 ,0)
                .end()
                .transform(FIXED)
                .rotation(0, 180, 0).scale(0.5f)
                .end()
                .transform(THIRDPERSON_RIGHT)
                .rotation(75, 315, 0).translation(0, 2.5f, 0).scale(0.375f)
                .end()
                .transform(FIRSTPERSON_RIGHT)
                .rotation(0, 315, 0).scale(0.4f)
                .end()
                .end();

        ModelFile statue = models().getBuilder("statue_block").texture("particle", mcLoc("block/stone"));
        getVariantBuilder(Registration.STATUE_BLOCK.get())
                .forAllStates(state ->
                        ConfiguredModel.builder().modelFile(statue).build()
                );
    }
}
