package cn.lcs.generate.repository;

import cn.lcs.generate.domain.GoOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface GoOrderRepository extends Repository<GoOrder, Integer> {

    public abstract GoOrder findById(int id);

    public abstract Page<GoOrder> findAll(Pageable page);
}
