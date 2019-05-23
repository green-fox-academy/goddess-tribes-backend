package com.greenfoxacademy.goddesstribesbackend.services;

import com.greenfoxacademy.goddesstribesbackend.models.ResourceType;
import com.greenfoxacademy.goddesstribesbackend.models.dtos.ResourceDTO;
import com.greenfoxacademy.goddesstribesbackend.models.dtos.ResourceTypeENUM;
import com.greenfoxacademy.goddesstribesbackend.models.dtos.ResourcesDTO;
import com.greenfoxacademy.goddesstribesbackend.models.entities.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class ProductionService {

  private BuildingService buildingService;
  private ResourceService resourceService;
  private SoldierService soldierService;

  @Autowired
  public ProductionService(BuildingService buildingService, ResourceService resourceService, SoldierService soldierService) {
    this.buildingService = buildingService;
    this.resourceService = resourceService;
    this.soldierService = soldierService;
  }

  public void updateResources(Long kingdomId) {
    ArrayList<Resource> resources = resourceService.findResourcesByKingdom(kingdomId);

    for (Resource resource : resources) {
      int previousAmount = resource.getAmount();
      int currentAmount = 0;

      if (resource.getType().equals(ResourceType.FOOD)) {
        currentAmount = previousAmount + (int) (calculateNetFoodGenerationRate(kingdomId) * calculateDuration(resource) / 60.);
        int maxFood = resource.getTownhall().getFoodCapacity();
        currentAmount = currentAmount <= maxFood ? currentAmount : maxFood;

      } else if (resource.getType().equals(ResourceType.GOLD)) {
        currentAmount = previousAmount + (int) (buildingService.calculateGoldGenerationRate(kingdomId) * calculateDuration(resource) / 60.);
        int maxGold = resource.getTownhall().getGoldCapacity();
        currentAmount = currentAmount <= maxGold ? currentAmount : maxGold;
      }

      resource.setAmount(currentAmount);
      resource.setUpdateTime(LocalDateTime.now());
      resourceService.save(resource);
    }
  }

  public int calculateNetFoodGenerationRate(Long kingdomId) {
    int foodGenerationRate = buildingService.calculateFoodGenerationRate(kingdomId);
    int foodConsumptionRate = soldierService.calculateFoodConsumptionRate(kingdomId);
    return foodGenerationRate - foodConsumptionRate;
  }

  private long calculateDuration(Resource resource) {
    LocalDateTime previousUpdateTime = resource.getUpdateTime();
    LocalDateTime now = LocalDateTime.now();
    long duration = Duration.between(previousUpdateTime, now).getSeconds();
    return duration;
  }

  public ResourcesDTO createResourcesDTO(Long kingdomId) {
    updateResources(kingdomId);

    ArrayList<ResourceDTO> resourceDTOS = new ArrayList<>();

    Resource foodResource = resourceService.findResourceByKingdomAndType(kingdomId, ResourceType.FOOD);
    int foodNetGenerationRate = calculateNetFoodGenerationRate(kingdomId);
    resourceDTOS.add(new ResourceDTO(ResourceTypeENUM.FOOD, foodResource.getAmount(), foodNetGenerationRate));

    Resource goldResource = resourceService.findResourceByKingdomAndType(kingdomId, ResourceType.GOLD);
    int goldGenerationRate = buildingService.calculateGoldGenerationRate(kingdomId);
    resourceDTOS.add(new ResourceDTO(ResourceTypeENUM.GOLD, goldResource.getAmount(), goldGenerationRate));

    return new ResourcesDTO(resourceDTOS);
  }

}
