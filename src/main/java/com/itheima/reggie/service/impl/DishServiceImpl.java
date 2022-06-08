package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;
    @Override
    @Transactional
    public void saveWithFlavor(DishDto dishDto) {
        //保存菜品基本信息
        this.baseMapper.insert(dishDto);
        //获取上面新增菜品的id
        Long dishId = dishDto.getId();

        //获取要保存的口味信息
        List<DishFlavor> flavors = dishDto.getFlavors();

        //循环遍历，为每个口味信息都添加dishId
        flavors.stream().forEach(flavor->{
            flavor.setDishId(dishId);
        });
        dishFlavorService.saveBatch(flavors);

    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id
     * @return
     */
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        //查询菜品基本信息,从dish表中查
        Dish dish = this.getById(id);
        //复制dish对象的值到dishDto对象
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish,dishDto);
        //查询当前菜品对应的口味信息,从dish_flavor表查询
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dish.getId());
        List<DishFlavor> flavors = dishFlavorService.list(queryWrapper);
        dishDto.setFlavors(flavors);
        return dishDto;
    }

    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        //更新dish
        this.updateById(dishDto);
        Long dishId = dishDto.getId();
        //获取flavors,并填充dish_flavor表里要更新的数据
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors.stream().forEach(flavor->{
            flavor.setDishId(dishId);
        });
        //删除dish_flavor表里原有的信息
        //构建条件器
        LambdaQueryWrapper<DishFlavor> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DishFlavor::getDishId,dishId);
        dishFlavorService.remove(queryWrapper);

        //重新插入新的要修改的口味信息
        dishFlavorService.saveBatch(flavors);

    }
}
