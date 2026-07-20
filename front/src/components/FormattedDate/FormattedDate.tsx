import Moment from 'react-moment';

type Props = {
  date: string;
};

export const FormattedDate = ({ date }: Props) => {
  return (
    <Moment local locale="ru" format="HH:mm DD.MM.YYYY" interval={0}>
      {date}
    </Moment>
  );
};

