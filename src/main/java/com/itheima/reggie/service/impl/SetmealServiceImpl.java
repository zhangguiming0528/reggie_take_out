package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 保存套餐信息,和套餐和菜品的关联关系
     * @param setmealDto
     */
    @Override
    @Transactional
    public void saveWithDish(SetmealDto setmealDto) {
        //保存套餐基本信息,到setmeal表
        this.save(setmealDto);
        //获取setmealId
        Long setmealId = setmealDto.getId();

        //把setmealId属性全部给设置进去
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().forEach(item->{
            item.setSetmealId(setmealId);
        });
        //批量保存套餐和菜品的关联关系,到setmeal_dish表
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 根据id查询套餐信息和对应的菜品信息
     * @param id
     * @return
     */
    @Override
    public SetmealDto getByIdWithsetmealDishes(Long id) {
        //查询套餐基本信息,从setmeal表中查
        Setmeal setmeal = this.getById(id);
        //复制dish对象的值到setmealDto对象
        SetmealDto setmealDto = new SetmealDto();
        BeanUtils.copyProperties(setmeal,setmealDto);
        //查询当前套餐对应的菜品信息,从setmeal_dish表查询
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmeal.getId());
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(list);
        return setmealDto;
    }

    /**
     * 修改套餐信息
     * @param setmealDto
     */
    @Override
    @Transactional
    public void updateWithSetmealDishes(SetmealDto setmealDto) {
        //更新setmeal
        this.updateById(setmealDto);
        //获取setmealId
        Long setmealId = setmealDto.getId();
        //获取setmealDishes,并填充setmeal_dishe表里要更新的数据
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //删除setmeal_dishe表里原有的信息
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId,setmealId);
        setmealDishService.remove(queryWrapper);

        //重新插入要修改的套餐和菜品的关联关系
        setmealDishService.saveBatch(setmealDishes);
    }

    /**
     * 根据ids删除套餐信息
     * @param ids
     */
    @Override
    @Transactional
    public void deleteWithSetmealDishes(List<Long> ids) {
        //查询传入的套餐id所属的套餐是否是停售状态
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Setmeal::getStatus,1);
        queryWrapper.in(Setmeal::getId,ids);

        int count = this.count(queryWrapper);
        //判断要删除的套餐里面有没有启售的套餐，有就不能删除
        if(count>0){
            //抛出业务异常
            throw new CustomException("有正在售卖的套餐，不能删除！");
        }
        //构建删除setmeal_dish表中关联关系
        LambdaQueryWrapper<SetmealDish> setmealDishQueryWrapper = new LambdaQueryWrapper<>();
        setmealDishQueryWrapper.in(SetmealDish::getSetmealId,ids);
        setmealDishService.remove(setmealDishQueryWrapper);
        //删除套餐
        this.removeByIds(ids);
    }


}
