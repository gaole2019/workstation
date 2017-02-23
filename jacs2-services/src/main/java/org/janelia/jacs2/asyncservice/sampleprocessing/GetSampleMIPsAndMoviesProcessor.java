package org.janelia.jacs2.asyncservice.sampleprocessing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.JacsServiceDataBuilder;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.asyncservice.common.AbstractServiceProcessor;
import org.janelia.jacs2.asyncservice.common.ComputationException;
import org.janelia.jacs2.asyncservice.common.ServiceComputation;
import org.janelia.jacs2.asyncservice.common.ServiceComputationFactory;
import org.janelia.jacs2.asyncservice.common.ServiceDataUtils;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GetSampleMIPsAndMoviesProcessor extends AbstractServiceProcessor<List<File>> {

    @Inject
    GetSampleMIPsAndMoviesProcessor(JacsServiceEngine jacsServiceEngine,
                                    ServiceComputationFactory computationFactory,
                                    JacsServiceDataPersistence jacsServiceDataPersistence,
                                    @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                    Logger logger) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public List<File> getResult(JacsServiceData jacsServiceData) {
        return ServiceDataUtils.stringToFileList(jacsServiceData.getStringifiedResult());
    }

    @Override
    public void setResult(List<File> result, JacsServiceData jacsServiceData) {
        jacsServiceData.setStringifiedResult(ServiceDataUtils.fileListToString(result));
    }

    @Override
    protected ServiceComputation<List<SampleImageFile>> preProcessData(JacsServiceData jacsServiceData) {
        JacsServiceData sampleLSMsServiceData = SampleServicesUtils.createChildSampleServiceData("getSampleImageFiles", getArgs(jacsServiceData), jacsServiceData);
        return createServiceComputation(jacsServiceEngine.submitSingleService(sampleLSMsServiceData))
                .thenCompose(sd -> this.waitForCompletion(sd))
                .thenApply(r -> ServiceDataUtils.stringToAny(sampleLSMsServiceData.getStringifiedResult(), new TypeReference<List<SampleImageFile>>() {
                }));
    }

    @Override
    protected ServiceComputation<List<File>> localProcessData(Object preProcessingResult, JacsServiceData jacsServiceData) {
        List<SampleImageFile> sampleLSMs = (List<SampleImageFile>) preProcessingResult;
        if (CollectionUtils.isEmpty(sampleLSMs)) {
            return computationFactory.newFailedComputation(new ComputationException(jacsServiceData, "No sample image file was found"));
        }
        GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args = getArgs(jacsServiceData);
        List<ServiceComputation<?>> mipsComputations = submitAllBasicMipsAndMoviesServices(sampleLSMs, args, jacsServiceData);
        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenCombineAll(mipsComputations, (sd, basicMipsResults) -> {
                    List<File> sampleMIPsResults = new ArrayList<>();
                    basicMipsResults.forEach(f -> sampleMIPsResults.addAll((List<File>) f));
                    return sampleMIPsResults;
                });
    }

    @Override
    protected boolean isResultAvailable(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected List<File> retrieveResult(Object preProcessingResult, JacsServiceData jacsServiceData) {
        throw new UnsupportedOperationException();
    }

    private List<ServiceComputation<?>> submitAllBasicMipsAndMoviesServices(List<SampleImageFile> lsmFiles, GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args, JacsServiceData jacsServiceData) {
        List<ServiceComputation<?>> basicMipsAndMoviesComputations = new ArrayList<>();
        lsmFiles.forEach(f -> {
            if (!f.isChanSpecDefined()) {
                throw new ComputationException(jacsServiceData,
                        "No channel spec for LSM " + f.getId() + "-" + f.getArchiveFilePath());
            }
            Path resultsDir = getResultsDir(args, f);
            JacsServiceDataBuilder basicMipsAndMoviesServiceBuilder =
                    new JacsServiceDataBuilder(jacsServiceData)
                            .setName("basicMIPsAndMovies")
                            .addArg("-imgFile", f.getWorkingFilePath())
                            .addArg("-chanSpec", f.getChanSpec())
                            .addArg("-colorSpec", f.getColorSpec());
            if (f.getLaser() != null) {
                basicMipsAndMoviesServiceBuilder.addArg("-laser", f.getLaser().toString());
            }
            if (f.getGain() != null) {
                basicMipsAndMoviesServiceBuilder.addArg("-gain", f.getGain().toString());
            }
            basicMipsAndMoviesServiceBuilder
                    .addArg("-options", args.options)
                    .addArg("-resultsDir", resultsDir.toString());
            basicMipsAndMoviesComputations.add(
                    createServiceComputation(jacsServiceEngine.submitSingleService(basicMipsAndMoviesServiceBuilder.build()))
                            .thenCompose(sd -> this.waitForCompletion(sd))
            );
        });
        return basicMipsAndMoviesComputations;
    }

    private Path getResultsDir(GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs args, SampleImageFile sampleImgFile) {
        ImmutableList.Builder<String> pathCompBuilder = new ImmutableList.Builder<>();
        if (StringUtils.isNotBlank(sampleImgFile.getObjective())) {
            pathCompBuilder.add(sampleImgFile.getObjective());
        }
        if (StringUtils.isNotBlank(sampleImgFile.getArea())) {
            pathCompBuilder.add(sampleImgFile.getArea());
        }
        pathCompBuilder.add(com.google.common.io.Files.getNameWithoutExtension(sampleImgFile.getWorkingFilePath()));
        pathCompBuilder.add(args.mipsSubDir);
        ImmutableList<String> pathComps = pathCompBuilder.build();
        return Paths.get(args.sampleDataDir, pathComps.toArray(new String[pathComps.size()]));
    }

    private GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs getArgs(JacsServiceData jacsServiceData) {
        return SampleServiceArgs.parse(jacsServiceData.getArgsArray(), new GetSampleMIPsAndMoviesServiceDescriptor.SampleMIPsAndMoviesArgs());
    }
}