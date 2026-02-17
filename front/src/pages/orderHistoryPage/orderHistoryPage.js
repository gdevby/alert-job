import React, { useEffect, useState } from 'react';
import Title from '../../components/common/title/Title';
import { parserService } from '../../services/parser/endponits/parserService';
import DropDownList from '../../components/common/dropDownList/DropDowList';
import Btn from '../../components/common/button/Button';
import TextField from '@mui/material/TextField';
import Select from '@mui/material/Select';
import MenuItem from '@mui/material/MenuItem';
import OrdersList from '../../components/orders/ordersList/OrdersList';
import Pagination from '@mui/material/Pagination';
import CircularProgress from '@mui/material/CircularProgress';
import InputLabel from '@mui/material/InputLabel';
import FormControl from '@mui/material/FormControl';
import '../../layouts/filtersPage/orders/orders.scss';
import './orderHistoryPage.scss';

const modes = [
  { id: 1, name: 'Название', value: 'TITLE' },
  { id: 2, name: 'Описание', value: 'DESCRIPTION' },
];

const orderHistoryPage = () => {
  const [isLoading, setIsLoading] = useState(false);
  const [sites, setSites] = useState([]);
  const [currentSites, setCurrentSites] = useState([]);
  const [selectedMode, setSelectedMode] = useState();
  const [keywords, setKeywords] = useState('');
  const [orders, setOrders] = useState();
  const [currentPage, setCurrentPage] = useState(1);

  useEffect(() => {
    parserService.getSites().then((response) => {
      setSites(response.data);
    });
  }, []);

  const handleChange = (event) => {
    const {
      target: { value },
    } = event;
    setCurrentSites(typeof value === 'string' ? value.split(',') : value);
  };

  const isFieldsValid = currentSites.length !== 0 && selectedMode !== undefined && keywords !== '';

  const handleSubmitButton = () => {
    if (!isFieldsValid) {
      return;
    }

    setIsLoading(true);
    setCurrentPage(1);

    parserService
      .getOrders(
        sites.map(({ name }) => name),
        selectedMode.value,
        keywords.split(' '),
        0,
        20,
      )
      .then((response) => {
        setOrders(response.data);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  const handlePagination = (_, value) => {
    setCurrentPage(value);

    setIsLoading(true);

    parserService
      .getOrders(
        sites.map(({ name }) => name),
        selectedMode.value,
        keywords.split(' '),
        value - 1,
        20,
      )
      .then((response) => {
        setOrders(response.data);
      })
      .finally(() => {
        setIsLoading(false);
      });
  };

  const ordersData =
    orders?.content?.map((order) => {
      return {
        ...order,
        sourceSite: {
          sourceName: new URL(order.link).hostname,
          categoryName: order.category,
          subCategoryName: order.subCategory,
        },
      };
    }) ?? [];

  return (
    <div className="container order-history">
      <Title text="История заказов" />
      <p>
        История заказов за большой период, чтобы понять какие из технологий популярны и в каких категориях и
        подкатегориях их искать
      </p>

      <div className="order-history__fields">
        <div className="order-history__sub-fields">
          <FormControl fullWidth size="small">
            <InputLabel id="demo-simple-select-label">Выберите сайт</InputLabel>
            <Select
              className="order-history__sites"
              value={currentSites}
              multiple
              label="Выберите сайт"
              onChange={handleChange}
            >
              {sites.map(({ name }) => (
                <MenuItem key={name} value={name}>
                  {name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <DropDownList
            className="order-history__mode"
            label={'Выберите раздел поиска'}
            elems={modes}
            onClick={setSelectedMode}
            defaultLabe={'Выберите раздел поиска'}
          />
        </div>

        <TextField
          className="order-history__keywords"
          label="Введите ключевые слова через пробел"
          variant="standard"
          placeholder="Введите ключевые слова через пробел"
          onChange={(event) => setKeywords(event.target.value)}
        />

        <div className="order-history__submit-button-with-pagination-container">
          <Btn
            className="order-history__submit-button"
            onClick={handleSubmitButton}
            text={'Искать'}
            variant="contained"
            disabled={!isFieldsValid || isLoading}
          />

          {orders?.totalPages > 0 && (
            <Pagination
              page={currentPage}
              count={orders?.totalPages}
              shape="rounded"
              onChange={handlePagination}
              disabled={!isFieldsValid || isLoading}
            />
          )}
        </div>
      </div>

      {!isLoading && ordersData.length > 0 && (
        <div className="order-history__table">
          <OrdersList orders={ordersData} />
        </div>
      )}
      {isLoading && (
        <div className="order-history__circular-progress">
          <CircularProgress />
        </div>
      )}
    </div>
  );
};

export default orderHistoryPage;
