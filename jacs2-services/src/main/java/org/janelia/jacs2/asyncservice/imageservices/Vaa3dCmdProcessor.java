package org.janelia.jacs2.asyncservice.imageservices;

import com.beust.jcommander.JCommander;
import com.google.common.collect.ImmutableMap;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.AbstractExeBasedServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ExternalCodeBlock;
import org.janelia.jacs2.asyncservice.common.ExternalProcessRunner;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.utils.ScriptWriter;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Map;

public class Vaa3dCmdProcessor extends AbstractExeBasedServiceProcessor<Void> {

    private final String vaa3dExecutable;
    private final String libraryPath;

    @Inject
    Vaa3dCmdProcessor(JacsServiceEngine jacsServiceEngine,
                      ServiceComputationFactory computationFactory,
                      JacsServiceDataPersistence jacsServiceDataPersistence,
                      @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                      @PropertyValue(name = "Executables.ModuleBase") String executablesBaseDir,
                      @Any Instance<ExternalProcessRunner> serviceRunners,
                      @PropertyValue(name = "VAA3D.Bin.Path") String vaa3dExecutable,
                      @PropertyValue(name = "VAA3D.LibraryPath") String libraryPath,
                      Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, executablesBaseDir, serviceRunners, logger);
        this.vaa3dExecutable = vaa3dExecutable;
        this.libraryPath = libraryPath;
    }

    @Override
    public Void getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public void setResult(Void result, JacsServiceData jacsServiceData) {
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return true;
    }

    @Override
    protected Void retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    protected ExternalCodeBlock prepareExternalScript(JacsServiceData jacsServiceData) {
        Vaa3dCmdServiceDescriptor.Vaa3dCmdArgs args = getArgs(jacsServiceData);
        ExternalCodeBlock externalScriptCode = new ExternalCodeBlock();
        ScriptWriter externalScriptWriter = externalScriptCode.getCodeWriter();
        createScript(args, externalScriptWriter);
        externalScriptWriter.close();
        return externalScriptCode;
    }

    private void createScript(Vaa3dCmdServiceDescriptor.Vaa3dCmdArgs args, ScriptWriter scriptWriter) {
        scriptWriter.addWithArgs(getVaa3dExecutable())
                .addArgs("-cmd", args.vaa3dCmd)
                .addArg(args.vaa3dCmdArgs).endArgs("");
    }

    @Override
    protected Map<String, String> prepareEnvironment(JacsServiceData jacsServiceData) {
        return ImmutableMap.of(DY_LIBRARY_PATH_VARNAME, getUpdatedEnvValue(DY_LIBRARY_PATH_VARNAME, libraryPath));
    }

    private Vaa3dCmdServiceDescriptor.Vaa3dCmdArgs getArgs(JacsServiceData jacsServiceData) {
        Vaa3dCmdServiceDescriptor.Vaa3dCmdArgs args = new Vaa3dCmdServiceDescriptor.Vaa3dCmdArgs();
        new JCommander(args).parse(jacsServiceData.getArgsArray());
        return args;
    }

    private String getVaa3dExecutable() {
        return getFullExecutableName(vaa3dExecutable);
    }

}