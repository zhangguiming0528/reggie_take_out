package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {

    //新增套餐
    void saveWithDish(SetmealDto setmealDto);

    //菜品信息回显
    SetmealDto getByIdWithsetmealDishes(Long id);

    //修改套餐信息
    void updateWithSetmealDishes(SetmealDto setmealDto);

    //删除套餐信息
    void deleteWithSetmealDishes(List<Long> ids);

}
