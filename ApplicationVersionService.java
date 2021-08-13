package com.mhealthatlas.apps.command;

import java.util.Optional;

import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mhealthatlas.apps.dto.AppScoreEventDto;
import com.mhealthatlas.apps.dto.AppVersionTaxonomyClassEventDto;
import com.mhealthatlas.apps.helper.TaxonomyClassHelper;
import com.mhealthatlas.apps.model.AndroidAppVersion;
import com.mhealthatlas.apps.model.AppType;
import com.mhealthatlas.apps.model.AppVersionTaxonomyClass;
import com.mhealthatlas.apps.model.IosAppVersion;
import com.mhealthatlas.apps.model.PrivateCustomerAppVersion;
import com.mhealthatlas.apps.model.TaxonomyClass;
import com.mhealthatlas.apps.repository.AndroidAppVersionRepository;
import com.mhealthatlas.apps.repository.AppVersionTaxonomyClassRepository;
import com.mhealthatlas.apps.repository.IosAppVersionRepository;
import com.mhealthatlas.apps.repository.PrivateAppVersionRepository;

import com.mhealthatlas.expertmapping.model.ApplicationQuestionExpert;
import com.mhealthatlas.expertmapping.model.ApplicationVersion;
import com.mhealthatlas.expertmapping.repository.ApplicationVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ApplicationVersionService implements IAppVersionCommandService {
  /** @hidden */
  @Autowired
  private ObjectMapper mapper;
  /** @hidden */
  @Autowired
  private ApplicationVersionRepository applicationVersionRepository;
  @Autowired
  private AndroidAppVersionRepository androidAppVersionRepository;
  /** @hidden */
  @Autowired
  private IosAppVersionRepository iosAppVersionRepository;
  /** @hidden */
  @Autowired
  private PrivateAppVersionRepository privateAppVersionRepository;
  /** @hidden */
  @Autowired
  private AppVersionTaxonomyClassRepository appVersionTaxonomyClassRepository;

  public  void handleNewAppVersion(JsonNode appData){
    try {
      ApplicationVersion applicationVersion = mapper.readValue(appData.asText(),
        ApplicationVersion.class);
      applicationVersion.setIsPayed(false);
      applicationVersionRepository.save(applicationVersion);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }
  public  void handleDeletedApp(JsonNode appData){
    try {
      ApplicationVersion applicationVersion = mapper.readValue(appData.asText(),
        ApplicationVersion.class);
      applicationVersion.setIsPayed(false);
      applicationVersionRepository.delete(applicationVersion);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }


  @Override
  @Transactional
  public void handleInsertedAppVersionTaxonomyClass(JsonNode appData, AppType type) throws JsonProcessingException {
    AndroidAppVersion androidAppVersion = null;
    IosAppVersion iosAppVersion = null;
    PrivateCustomerAppVersion privateAppVersion = null;

    AppVersionTaxonomyClassEventDto appVersionTaxonomyClass = mapper.treeToValue(mapper.readTree(appData.asText()),
      AppVersionTaxonomyClassEventDto.class);
    switch (type) {
      case Android:
        Optional<AndroidAppVersion> aAppVersion = androidAppVersionRepository
          .findById(appVersionTaxonomyClass.getAppVersionId());
        if (aAppVersion.isPresent()) {
          androidAppVersion = aAppVersion.get();
        }
        break;
      case Ios:
        Optional<IosAppVersion> iAppVersion = iosAppVersionRepository
          .findById(appVersionTaxonomyClass.getAppVersionId());
        if (iAppVersion.isPresent()) {
          iosAppVersion = iAppVersion.get();
        }
        break;
      case Private:
        Optional<PrivateCustomerAppVersion> pAppVersion = privateAppVersionRepository
          .findById(appVersionTaxonomyClass.getAppVersionId());
        if (pAppVersion.isPresent()) {
          privateAppVersion = pAppVersion.get();
        }
        break;
      default:
        break;
    }

    Optional<TaxonomyClass> taxonomyClass = TaxonomyClassHelper.validate(appVersionTaxonomyClass);
    if (taxonomyClass.isPresent()) {
      appVersionTaxonomyClassRepository.save(new AppVersionTaxonomyClass(null, androidAppVersion, iosAppVersion,
        privateAppVersion, taxonomyClass.get(), 0.0, 0.0, 0.0, 0.0, 0.0));
    }
  }

  @Override
  @Transactional
  public void handleDeletedAppVersionTaxonomyClass(JsonNode appData, AppType type) throws JsonProcessingException {
    Optional<AppVersionTaxonomyClass> avtc = Optional.empty();

    AppVersionTaxonomyClassEventDto appVersionTaxonomyClass = mapper.treeToValue(mapper.readTree(appData.asText()),
      AppVersionTaxonomyClassEventDto.class);

    Optional<TaxonomyClass> taxonomyClass = TaxonomyClassHelper.validate(appVersionTaxonomyClass);
    if (taxonomyClass.isEmpty()) {
      return;
    }

    switch (type) {
      case Android:
        Optional<AndroidAppVersion> aAppVersion = androidAppVersionRepository
          .findById(appVersionTaxonomyClass.getAppVersionId());
        if (aAppVersion.isPresent()) {

          avtc = aAppVersion.get().getAppVersionTaxonomyClasses().stream()
            .filter(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId()).findFirst();
          aAppVersion.get().getAppVersionTaxonomyClasses()
            .removeIf(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId());
        }
        break;
      case Ios:
        Optional<IosAppVersion> iAppVersion = iosAppVersionRepository
          .findById(appVersionTaxonomyClass.getAppVersionId());
        if (iAppVersion.isPresent()) {
          avtc = iAppVersion.get().getAppVersionTaxonomyClasses().stream()
            .filter(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId()).findFirst();
          iAppVersion.get().getAppVersionTaxonomyClasses()
            .removeIf(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId());
        }
        break;
      case Private:
        Optional<PrivateCustomerAppVersion> pAppVersion = privateAppVersionRepository
          .findById(appVersionTaxonomyClass.getAppVersionId());
        if (pAppVersion.isPresent()) {
          avtc = pAppVersion.get().getAppVersionTaxonomyClasses().stream()
            .filter(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId()).findFirst();
          pAppVersion.get().getAppVersionTaxonomyClasses()
            .removeIf(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId());
        }
        break;
      default:
        break;
    }

    if (avtc.isPresent()) {
      appVersionTaxonomyClassRepository.delete(avtc.get());
    }
  }

  @Override
  @Transactional
  public void handleChangedAppVersionTaxonomyClassScore(JsonNode appData) throws JsonProcessingException {
    AppScoreEventDto appScore = mapper.treeToValue(mapper.readTree(appData.asText()), AppScoreEventDto.class);
    switch (appScore.getAppType()) {
      case Android:
        Optional<AndroidAppVersion> androidAppVersion = androidAppVersionRepository
          .findById(appScore.getAppVersionId());
        if (androidAppVersion.isPresent()) {
          Optional<TaxonomyClass> taxonomyClass = TaxonomyClassHelper.validate(appScore.getTaxonomyClass());
          if (taxonomyClass.isPresent()) {
            Optional<AppVersionTaxonomyClass> appVersionTaxonomyClass = androidAppVersion.get()
              .getAppVersionTaxonomyClasses().stream()
              .filter(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId()).findFirst();
            if (appVersionTaxonomyClass.isPresent()) {
              appVersionTaxonomyClass.get().setTotalScore(appScore.getScore());
              appVersionTaxonomyClass.get().setContentScore(appScore.getContentScore());
              appVersionTaxonomyClass.get().setUsabilityScore(appScore.getUsabilityScore());
              appVersionTaxonomyClass.get().setSecurityScore(appScore.getSecurityScore());
              appVersionTaxonomyClass.get().setLawScore(appScore.getLawScore());
              appVersionTaxonomyClassRepository.save(appVersionTaxonomyClass.get());
            }
          }
        }
        break;
      case Ios:
        Optional<IosAppVersion> iosAppVersion = iosAppVersionRepository.findById(appScore.getAppVersionId());
        if (iosAppVersion.isPresent()) {
          Optional<TaxonomyClass> taxonomyClass = TaxonomyClassHelper.validate(appScore.getTaxonomyClass());
          if (taxonomyClass.isPresent()) {
            Optional<AppVersionTaxonomyClass> appVersionTaxonomyClass = iosAppVersion.get()
              .getAppVersionTaxonomyClasses().stream()
              .filter(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId()).findFirst();
            if (appVersionTaxonomyClass.isPresent()) {
              appVersionTaxonomyClass.get().setTotalScore(appScore.getScore());
              appVersionTaxonomyClass.get().setContentScore(appScore.getContentScore());
              appVersionTaxonomyClass.get().setUsabilityScore(appScore.getUsabilityScore());
              appVersionTaxonomyClass.get().setSecurityScore(appScore.getSecurityScore());
              appVersionTaxonomyClass.get().setLawScore(appScore.getLawScore());
              appVersionTaxonomyClassRepository.save(appVersionTaxonomyClass.get());
            }
          }
        }
        break;
      case Private:
        Optional<PrivateCustomerAppVersion> privateAppVersion = privateAppVersionRepository
          .findById(appScore.getAppVersionId());
        if (privateAppVersion.isPresent()) {
          Optional<TaxonomyClass> taxonomyClass = TaxonomyClassHelper.validate(appScore.getTaxonomyClass());
          if (taxonomyClass.isPresent()) {
            Optional<AppVersionTaxonomyClass> appVersionTaxonomyClass = privateAppVersion.get()
              .getAppVersionTaxonomyClasses().stream()
              .filter(x -> x.getTaxonomyClass().getId() == taxonomyClass.get().getId()).findFirst();
            if (appVersionTaxonomyClass.isPresent()) {
              appVersionTaxonomyClass.get().setTotalScore(appScore.getScore());
              appVersionTaxonomyClass.get().setContentScore(appScore.getContentScore());
              appVersionTaxonomyClass.get().setUsabilityScore(appScore.getUsabilityScore());
              appVersionTaxonomyClass.get().setSecurityScore(appScore.getSecurityScore());
              appVersionTaxonomyClass.get().setLawScore(appScore.getLawScore());
              appVersionTaxonomyClassRepository.save(appVersionTaxonomyClass.get());
            }
          }
        }
        break;
      default:
        break;
    }
  }
}
